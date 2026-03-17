<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import axios from 'axios';

const emit = defineEmits(['graph-updated', 'algorithm-complete', 'loading']);

const canvas = ref(null);
const ctx = ref(null);

const nodes = ref([]);
const edges = ref([]);
const selectedNodeId = ref(null);
const pathNodeIds = ref([]);
const needsRedraw = ref(true);
const animationFrameId = ref(null);

const apiBase = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api/graphs';
const graphId = ref(null);
const isDirected = ref(false);

// Drag state
const draggingNode = ref(null);
const isDragging = ref(false);
const dragStartPos = ref({ x: 0, y: 0 });

// Zoom/Pan state
const camera = ref({ x: 0, y: 0, zoom: 1 });
const isPanning = ref(false);
const panStart = ref({ x: 0, y: 0 });
const panCameraStart = ref({ x: 0, y: 0 });

const NODE_RADIUS = 20;
const MIN_ZOOM = 0.3;
const MAX_ZOOM = 3.0;
const ZOOM_SPEED = 0.001;

// Generate random UUID v4
const uuidv4 = () => {
    return "10000000-1000-4000-8000-100000000000".replace(/[018]/g, c => {
        const byte = crypto.getRandomValues(new Uint8Array(1))[0];
        return (Number(c) ^ (byte & (15 >> (Number(c) / 4)))).toString(16);
    });
};

const requestRedraw = () => {
    needsRedraw.value = true;
};

// Convert screen coords to world coords
const screenToWorld = (sx, sy) => {
    return {
        x: (sx - camera.value.x) / camera.value.zoom,
        y: (sy - camera.value.y) / camera.value.zoom
    };
};

// Convert world coords to screen coords
const worldToScreen = (wx, wy) => {
    return {
        x: wx * camera.value.zoom + camera.value.x,
        y: wy * camera.value.zoom + camera.value.y
    };
};

// Initialize Canvas
onMounted(async () => {
    window.addEventListener('resize', resizeCanvas);
    if (canvas.value) {
        ctx.value = canvas.value.getContext('2d');
        canvas.value.addEventListener('wheel', handleWheel, { passive: false });
        resizeCanvas();
        animate();
    }
    await seedRandomGraph();
});

onUnmounted(() => {
    window.removeEventListener('resize', resizeCanvas);
    if (canvas.value) {
        canvas.value.removeEventListener('wheel', handleWheel);
    }
    if (animationFrameId.value) {
        cancelAnimationFrame(animationFrameId.value);
    }
});

const resizeCanvas = () => {
    if (!canvas.value) return;
    const parent = canvas.value.parentElement;
    canvas.value.width = parent.clientWidth;
    canvas.value.height = parent.clientHeight;
    requestRedraw();
};

const createGraph = async (directed = false) => {
    try {
        const response = await axios.post(`${apiBase}?directed=${directed}`);
        graphId.value = response.data.id;
        isDirected.value = directed;
    } catch (e) {
        console.error("Error creating graph on backend.", e);
    }
}

// Draw Loop
const draw = () => {
    if (!ctx.value || !canvas.value) return;
    const c = ctx.value;
    const cam = camera.value;

    c.clearRect(0, 0, canvas.value.width, canvas.value.height);
    c.save();
    c.translate(cam.x, cam.y);
    c.scale(cam.zoom, cam.zoom);

    // Draw edges
    edges.value.forEach(edge => {
        const source = nodes.value.find(n => n.id === edge.source);
        const target = nodes.value.find(n => n.id === edge.target);

        if (source && target) {
            c.beginPath();
            c.moveTo(source.x, source.y);
            c.lineTo(target.x, target.y);

            const sourceIndex = pathNodeIds.value.indexOf(source.id);
            const targetIndex = pathNodeIds.value.indexOf(target.id);

            if (sourceIndex !== -1 && targetIndex !== -1 && Math.abs(sourceIndex - targetIndex) === 1) {
                c.strokeStyle = '#ef4444';
                c.lineWidth = 4 / cam.zoom;
            } else {
                c.strokeStyle = '#94a3b8';
                c.lineWidth = 2 / cam.zoom;
            }

            c.stroke();

            // Draw Weight
            const midX = (source.x + target.x) / 2;
            const midY = (source.y + target.y) / 2;
            const fontSize = Math.max(10, 14 / cam.zoom);
            c.fillStyle = 'rgba(255,255,255,0.7)';
            c.font = `${fontSize}px Inter`;
            c.fillText(edge.weight.toFixed(1), midX + 10 / cam.zoom, midY - 10 / cam.zoom);
        }
    });

    // Draw nodes
    nodes.value.forEach(node => {
        const r = NODE_RADIUS / cam.zoom;

        c.beginPath();
        c.arc(node.x, node.y, r, 0, Math.PI * 2);

        if (draggingNode.value === node.id) {
            c.fillStyle = '#22d3ee'; // Cyan while dragging
            c.shadowBlur = 20 / cam.zoom;
            c.shadowColor = '#22d3ee';
        } else if (selectedNodeId.value === node.id) {
            c.fillStyle = '#f59e0b';
            c.shadowBlur = 15 / cam.zoom;
            c.shadowColor = '#f59e0b';
        } else if (pathNodeIds.value.includes(node.id)) {
            c.fillStyle = '#ef4444';
            c.shadowBlur = 15 / cam.zoom;
            c.shadowColor = '#ef4444';
        } else {
            c.fillStyle = '#3b82f6';
            c.shadowBlur = 0;
        }

        c.fill();
        c.strokeStyle = 'rgba(255,255,255,0.8)';
        c.lineWidth = 2 / cam.zoom;
        c.stroke();

        c.shadowBlur = 0;

        const fontSize = Math.max(8, 12 / cam.zoom);
        c.fillStyle = '#fff';
        c.font = `${fontSize}px Inter`;
        c.textAlign = 'center';
        c.textBaseline = 'middle';
        c.fillText(node.label, node.x, node.y);
    });

    c.restore();

    // Draw zoom indicator (screen space)
    const zoomPct = Math.round(cam.zoom * 100);
    c.fillStyle = 'rgba(255,255,255,0.3)';
    c.font = '12px Inter';
    c.textAlign = 'right';
    c.textBaseline = 'bottom';
    c.fillText(`${zoomPct}%`, canvas.value.width - 16, canvas.value.height - 16);
};

const animate = () => {
    if (needsRedraw.value) {
        draw();
        needsRedraw.value = false;
    }
    animationFrameId.value = requestAnimationFrame(animate);
};

// Find node at world coordinates
const findNodeAt = (wx, wy) => {
    const hitRadius = NODE_RADIUS / camera.value.zoom;
    return nodes.value.find(n => {
        const dx = n.x - wx;
        const dy = n.y - wy;
        return Math.sqrt(dx * dx + dy * dy) < hitRadius;
    });
};

// Mouse handlers
const handleMouseDown = (e) => {
    if (!canvas.value) return;
    const rect = canvas.value.getBoundingClientRect();
    const sx = e.clientX - rect.left;
    const sy = e.clientY - rect.top;

    // Middle mouse button or ctrl+click for panning
    if (e.button === 1 || (e.button === 0 && e.ctrlKey)) {
        isPanning.value = true;
        panStart.value = { x: sx, y: sy };
        panCameraStart.value = { x: camera.value.x, y: camera.value.y };
        e.preventDefault();
        return;
    }

    if (e.button !== 0) return;

    const world = screenToWorld(sx, sy);
    const clickedNode = findNodeAt(world.x, world.y);

    if (clickedNode) {
        draggingNode.value = clickedNode.id;
        isDragging.value = false;
        dragStartPos.value = { x: world.x, y: world.y };
        requestRedraw();
    }
};

const handleMouseMove = (e) => {
    if (!canvas.value) return;
    const rect = canvas.value.getBoundingClientRect();
    const sx = e.clientX - rect.left;
    const sy = e.clientY - rect.top;

    // Panning
    if (isPanning.value) {
        camera.value.x = panCameraStart.value.x + (sx - panStart.value.x);
        camera.value.y = panCameraStart.value.y + (sy - panStart.value.y);
        requestRedraw();
        return;
    }

    // Dragging node
    if (draggingNode.value) {
        const world = screenToWorld(sx, sy);
        const dx = world.x - dragStartPos.value.x;
        const dy = world.y - dragStartPos.value.y;

        if (!isDragging.value && Math.sqrt(dx * dx + dy * dy) > 3) {
            isDragging.value = true;
        }

        if (isDragging.value) {
            const node = nodes.value.find(n => n.id === draggingNode.value);
            if (node) {
                node.x = world.x;
                node.y = world.y;
                requestRedraw();
            }
        }
    }
};

const handleMouseUp = (e) => {
    if (!canvas.value) return;

    // End panning
    if (isPanning.value) {
        isPanning.value = false;
        return;
    }

    const rect = canvas.value.getBoundingClientRect();
    const sx = e.clientX - rect.left;
    const sy = e.clientY - rect.top;
    const world = screenToWorld(sx, sy);

    if (draggingNode.value) {
        if (isDragging.value) {
            // Was a drag — just release, emit updated positions
            emit('graph-updated', nodes.value);
        } else {
            // Was a click on a node (no drag)
            const clickedNode = findNodeAt(world.x, world.y);
            if (clickedNode) {
                if (selectedNodeId.value && selectedNodeId.value !== clickedNode.id) {
                    createEdge(selectedNodeId.value, clickedNode.id);
                    selectedNodeId.value = null;
                } else if (selectedNodeId.value === clickedNode.id) {
                    selectedNodeId.value = null;
                } else {
                    selectedNodeId.value = clickedNode.id;
                }
            }
            pathNodeIds.value = [];
        }

        draggingNode.value = null;
        isDragging.value = false;
        requestRedraw();
        return;
    }

    // Clicked on empty space — create node
    if (e.button === 0 && !e.ctrlKey) {
        const newNodeId = uuidv4();
        nodes.value.push({
            id: newNodeId,
            label: "N" + (nodes.value.length + 1),
            x: world.x,
            y: world.y
        });
        emit('graph-updated', nodes.value);
        selectedNodeId.value = null;
        pathNodeIds.value = [];
        requestRedraw();
    }
};

const handleWheel = (e) => {
    e.preventDefault();
    if (!canvas.value) return;

    const rect = canvas.value.getBoundingClientRect();
    const sx = e.clientX - rect.left;
    const sy = e.clientY - rect.top;

    const oldZoom = camera.value.zoom;
    const delta = -e.deltaY * ZOOM_SPEED;
    const newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, oldZoom * (1 + delta)));

    // Zoom toward cursor position
    const worldBeforeX = (sx - camera.value.x) / oldZoom;
    const worldBeforeY = (sy - camera.value.y) / oldZoom;

    camera.value.zoom = newZoom;
    camera.value.x = sx - worldBeforeX * newZoom;
    camera.value.y = sy - worldBeforeY * newZoom;

    requestRedraw();
};

const handleContextMenu = (e) => {
    e.preventDefault(); // Prevent right-click menu on canvas
};

// Edge creation
const createEdge = async (sourceId, targetId) => {
    const duplicate = isDirected.value
        ? edges.value.find(e => e.source === sourceId && e.target === targetId)
        : edges.value.find(e =>
            (e.source === sourceId && e.target === targetId) ||
            (e.source === targetId && e.target === sourceId));

    if (duplicate) return;

    const source = nodes.value.find(n => n.id === sourceId);
    const target = nodes.value.find(n => n.id === targetId);
    if (!source || !target) return;

    const dx = source.x - target.x;
    const dy = source.y - target.y;
    const distance = Math.sqrt(dx * dx + dy * dy) / 10;

    edges.value.push({ source: sourceId, target: targetId, weight: distance });
    requestRedraw();

    if (graphId.value) {
        await axios.post(`${apiBase}/${graphId.value}/edges`, [{
            source: sourceId,
            sourceProps: { x: source.x, y: source.y },
            target: targetId,
            targetProps: { x: target.x, y: target.y },
            weight: distance
        }]);
    }
};

const lastExecutionTime = ref(null);

const runAlgorithm = async (algo, explicitSourceId, explicitTargetId) => {
    if (!graphId.value || nodes.value.length < 2) return;

    const sourceNode = explicitSourceId || nodes.value[0].id;
    const targetNode = explicitTargetId || nodes.value[nodes.value.length - 1].id;

    emit('loading', true);

    try {
        let url = `${apiBase}/${graphId.value}/algorithms/`;
        const params = new URLSearchParams();

        if (algo === 'dijkstra') { url += 'shortest-path/dijkstra'; params.set('source', sourceNode); params.set('target', targetNode); }
        else if (algo === 'astar') { url += 'shortest-path/astar'; params.set('source', sourceNode); params.set('target', targetNode); }
        else if (algo === 'bfs') { url += 'traversal/bfs'; params.set('source', sourceNode); }
        else if (algo === 'dfs') { url += 'traversal/dfs'; params.set('source', sourceNode); }
        else if (algo === 'pagerank') { url += 'pagerank'; }
        else if (algo === 'louvain') { url += 'community/louvain'; }
        else if (algo === 'closeness') { url += 'centrality/closeness'; }
        else if (algo === 'betweenness') { url += 'centrality/betweenness'; }

        const queryString = params.toString();
        if (queryString) url += '?' + queryString;

        const response = await axios.post(url);
        const resultData = response.data.result;
        lastExecutionTime.value = response.data.executionTimeMs;

        if (algo === 'dijkstra' || algo === 'astar') {
            pathNodeIds.value = resultData?.pathNodeIds || [];
        } else if (algo === 'bfs' || algo === 'dfs') {
            pathNodeIds.value = Array.isArray(resultData) ? resultData : [];
        } else {
            pathNodeIds.value = nodes.value.map(n => n.id);
        }

        requestRedraw();
        emit('algorithm-complete', { algo, time: response.data.executionTimeMs, result: resultData });
    } catch (e) {
        console.error("Algo Failed", e);
        pathNodeIds.value = [];
        lastExecutionTime.value = null;
        requestRedraw();
        emit('algorithm-complete', { algo, time: null, result: null, error: e.response?.data || 'Algorithm failed' });
    } finally {
        emit('loading', false);
    }
};

const clearGraph = async () => {
    nodes.value = [];
    edges.value = [];
    pathNodeIds.value = [];
    selectedNodeId.value = null;
    lastExecutionTime.value = null;
    camera.value = { x: 0, y: 0, zoom: 1 };
    requestRedraw();
    await createGraph();
};

const resetView = () => {
    camera.value = { x: 0, y: 0, zoom: 1 };
    requestRedraw();
};

const scatterNodes = (serverNodes) => {
    const positions = [];
    const minDistance = 45;
    const maxAttempts = 500;

    let w = (canvas.value && canvas.value.width > 200) ? canvas.value.width : 800;
    let h = (canvas.value && canvas.value.height > 200) ? canvas.value.height : 600;

    return serverNodes.map((n) => {
        let x, y;
        let attempt = 0;
        let validPosition = false;

        while (!validPosition && attempt < maxAttempts) {
            x = 40 + Math.random() * (w - 80);
            y = 40 + Math.random() * (h - 80);

            validPosition = true;
            for (const pos of positions) {
                const dx = x - pos.x;
                const dy = y - pos.y;
                if (Math.sqrt(dx * dx + dy * dy) < minDistance) {
                    validPosition = false;
                    break;
                }
            }
            attempt++;
        }
        positions.push({ x, y });

        return {
            id: n.id,
            label: n.id.substring(0, 4),
            x: x,
            y: y
        };
    });
};

const seedRandomGraph = async () => {
    emit('loading', true);
    try {
        const response = await axios.post(`${apiBase}/seed/random?nodes=30&edges=60`);
        graphId.value = response.data.id;

        const nodesReq = await axios.get(`${apiBase}/${graphId.value}/nodes`);
        const edgesReq = await axios.get(`${apiBase}/${graphId.value}/edges`);

        nodes.value = scatterNodes(nodesReq.data || []);
        edges.value = (edgesReq.data || []).map(e => ({
            source: e.sourceId,
            target: e.targetId,
            weight: e.weight
        }));

        pathNodeIds.value = [];
        selectedNodeId.value = null;
        lastExecutionTime.value = null;
        camera.value = { x: 0, y: 0, zoom: 1 };

        requestRedraw();
        emit('graph-updated', nodes.value);
    } catch (e) {
        console.error("Failed to seed graph", e);
    } finally {
        emit('loading', false);
    }
}

const loadGraphId = async (id) => {
    graphId.value = id;
    emit('loading', true);
    try {
        const nodesReq = await axios.get(`${apiBase}/${graphId.value}/nodes`);
        const edgesReq = await axios.get(`${apiBase}/${graphId.value}/edges`);

        nodes.value = scatterNodes(nodesReq.data || []);
        edges.value = (edgesReq.data || []).map(e => ({
            source: e.sourceId, target: e.targetId, weight: e.weight
        }));

        requestRedraw();
        emit('graph-updated', nodes.value);
        pathNodeIds.value = [];
        selectedNodeId.value = null;
        lastExecutionTime.value = null;
    } catch (e) {
        console.error("Load failed", e);
    } finally {
        emit('loading', false);
    }
}

defineExpose({ runAlgorithm, clearGraph, seedRandomGraph, loadGraphId, resetView, lastExecutionTime, graphId });
</script>

<template>
    <div class="canvas-container">
        <canvas
            ref="canvas"
            @mousedown="handleMouseDown"
            @mousemove="handleMouseMove"
            @mouseup="handleMouseUp"
            @mouseleave="handleMouseUp"
            @contextmenu="handleContextMenu"
        ></canvas>
        <div class="instructions glass-panel">
            <p><strong>Click empty space</strong> to create nodes.</p>
            <p><strong>Click a node, then another</strong> to create an edge.</p>
            <p><strong>Drag a node</strong> to reposition it.</p>
            <p><strong>Scroll</strong> to zoom. <strong>Ctrl+drag</strong> to pan.</p>
        </div>
    </div>
</template>

<style scoped>
.canvas-container {
    width: 100%;
    height: 100%;
    position: relative;
    background: radial-gradient(circle at center, #1e293b 0%, #0f172a 100%);
    border-radius: 16px;
    overflow: hidden;
}

canvas {
    width: 100%;
    height: 100%;
    cursor: crosshair;
}

.instructions {
    position: absolute;
    bottom: 20px;
    left: 20px;
    text-align: left;
    padding: 1rem 1.5rem;
    pointer-events: none;
}

.instructions p {
    margin: 0.25rem 0;
    font-size: 0.85rem;
    color: #cbd5e1;
}
</style>
