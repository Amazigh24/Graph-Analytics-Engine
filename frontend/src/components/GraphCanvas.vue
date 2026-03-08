<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue';
import axios from 'axios';

const emit = defineEmits(['graph-updated', 'algorithm-complete']);

const canvas = ref(null);
const ctx = ref(null);

const nodes = ref([]);
const edges = ref([]);
const selectedNodeId = ref(null);
const pathNodeIds = ref([]);

const apiBase = 'http://localhost:8080/api/graphs';
const graphId = ref(null);

// Generate random UUID for nodes
const uuidv4 = () => {
    return "10000000-1000-4000-8000-100000000000".replace(/[018]/g, c =>
        (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
    );
};

// Initialize Canvas
onMounted(async () => {
    window.addEventListener('resize', resizeCanvas);
    if (canvas.value) {
        ctx.value = canvas.value.getContext('2d');
        resizeCanvas();
        animate();
    }
    await seedRandomGraph(); // Instantly seed to avoid blank canvas
});

onUnmounted(() => {
    window.removeEventListener('resize', resizeCanvas);
});

const resizeCanvas = () => {
    if (!canvas.value) return;
    const parent = canvas.value.parentElement;
    canvas.value.width = parent.clientWidth;
    canvas.value.height = parent.clientHeight;
    draw();
};

const createGraph = async () => {
    try {
        const response = await axios.post(`${apiBase}?directed=false`);
        graphId.value = response.data.id;
        console.log("Graph Created: ", graphId.value);
    } catch (e) {
        console.error("Error creating graph on backend.", e);
    }
}

// Draw Loop
const draw = () => {
    if (!ctx.value || !canvas.value) return;
    
    // Clear canvas
    ctx.value.clearRect(0, 0, canvas.value.width, canvas.value.height);
    
    // Draw edges
    edges.value.forEach(edge => {
        const source = nodes.value.find(n => n.id === edge.source);
        const target = nodes.value.find(n => n.id === edge.target);
        
        if(source && target) {
            ctx.value.beginPath();
            ctx.value.moveTo(source.x, source.y);
            ctx.value.lineTo(target.x, target.y);
            
            // Check if edge is part of the path
            const sourceIndex = pathNodeIds.value.indexOf(source.id);
            const targetIndex = pathNodeIds.value.indexOf(target.id);
            
            if (sourceIndex !== -1 && targetIndex !== -1 && Math.abs(sourceIndex - targetIndex) === 1) {
                ctx.value.strokeStyle = '#ef4444'; // Red path
                ctx.value.lineWidth = 4;
            } else {
                ctx.value.strokeStyle = '#94a3b8'; // Grey edge
                ctx.value.lineWidth = 2;
            }
            
            ctx.value.stroke();
            
            // Draw Weight
            const midX = (source.x + target.x) / 2;
            const midY = (source.y + target.y) / 2;
            ctx.value.fillStyle = '#fff';
            ctx.value.font = '14px Inter';
            ctx.value.fillText(edge.weight.toFixed(1), midX + 10, midY - 10);
        }
    });
    
    // Draw nodes
    nodes.value.forEach(node => {
        ctx.value.beginPath();
        ctx.value.arc(node.x, node.y, 20, 0, Math.PI * 2);
        
        if (selectedNodeId.value === node.id) {
            ctx.value.fillStyle = '#f59e0b'; // Amber Selected
            ctx.value.shadowBlur = 15;
            ctx.value.shadowColor = '#f59e0b';
        } else if (pathNodeIds.value.includes(node.id)) {
            ctx.value.fillStyle = '#ef4444'; // Red for Path
            ctx.value.shadowBlur = 15;
            ctx.value.shadowColor = '#ef4444';
        } else {
            ctx.value.fillStyle = '#3b82f6'; // Blue Default
            ctx.value.shadowBlur = 0;
        }
        
        ctx.value.fill();
        ctx.value.strokeStyle = 'rgba(255,255,255,0.8)';
        ctx.value.lineWidth = 2;
        ctx.value.stroke();
        
        ctx.value.fillStyle = '#fff';
        ctx.value.font = '12px Inter';
        ctx.value.textAlign = 'center';
        ctx.value.textBaseline = 'middle';
        ctx.value.fillText(node.label, node.x, node.y);
    });
};

const animate = () => {
    draw();
    requestAnimationFrame(animate);
};

// Interactions
const handleCanvasClick = (e) => {
    if (!canvas.value) return;
    
    const rect = canvas.value.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    
    // Check if clicked on a node
    const clickedNode = nodes.value.find(n => {
        const dx = n.x - x;
        const dy = n.y - y;
        return Math.sqrt(dx*dx + dy*dy) < 20;
    });
    
    if (clickedNode) {
        if (selectedNodeId.value) {
            if (selectedNodeId.value !== clickedNode.id) {
                // Create Edge between selected and clicked
                createEdge(selectedNodeId.value, clickedNode.id);
            }
            selectedNodeId.value = null; // Deselect
        } else {
            selectedNodeId.value = clickedNode.id; // Select
        }
    } else {
        // Create new Node
        const newNodeId = uuidv4();
        nodes.value.push({
            id: newNodeId,
            label: "N" + (nodes.value.length + 1),
            x: x,
            y: y
        });
        emit('graph-updated', nodes.value);
        
        selectedNodeId.value = null;
    }
    
    pathNodeIds.value = []; // Reset path when interacting
};

const createEdge = async (sourceId, targetId) => {
    // Check if edge already exists
    if (edges.value.find(e => (e.source === sourceId && e.target === targetId) || (e.source === targetId && e.target === sourceId))) {
        return;
    }

    const source = nodes.value.find(n => n.id === sourceId);
    const target = nodes.value.find(n => n.id === targetId);
    const dx = source.x - target.x;
    const dy = source.y - target.y;
    const distance = Math.sqrt(dx*dx + dy*dy) / 10; // Scaled dist
    
    edges.value.push({ source: sourceId, target: targetId, weight: distance });
    
    // Send edges to backend
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
    
    // Use explicit IDs from UI, fallback to array bounds if undefined (e.g. testing)
    const sourceNode = explicitSourceId || nodes.value[0].id;
    const targetNode = explicitTargetId || nodes.value[nodes.value.length - 1].id;
    
    try {
        let url = `${apiBase}/${graphId.value}/algorithms/`;
        // Map frontend selection to correct backend path
        if (algo === 'dijkstra') url += `shortest-path/dijkstra?source=${sourceNode}&target=${targetNode}`;
        if (algo === 'astar') url += `shortest-path/astar?source=${sourceNode}&target=${targetNode}`;
        if (algo === 'bfs') url += `traversal/bfs?source=${sourceNode}`;
        if (algo === 'dfs') url += `traversal/dfs?source=${sourceNode}`;
        if (algo === 'pagerank') url += `pagerank`;
        if (algo === 'louvain') url += `community/louvain`;
        if (algo === 'closeness') url += `centrality/closeness`;
        if (algo === 'betweenness') url += `centrality/betweenness`;

        const response = await axios.post(url);
        
        // Extract from AlgorithmResultWrapper
        const resultData = response.data.result;
        lastExecutionTime.value = response.data.executionTimeMs;
        
        if (algo === 'dijkstra' || algo === 'astar') {
            pathNodeIds.value = resultData.pathNodeIds;
            console.log("Path:", pathNodeIds.value);
        } else if (algo === 'bfs' || algo === 'dfs') {
            pathNodeIds.value = resultData; // It returns a list of nodes
            console.log("Traversal Order:", pathNodeIds.value);
        } else {
             // For pageRank, Louvain, Centrality - we'll just flash them to show completion since they return Maps (NodeID -> Value/Community)
             pathNodeIds.value = nodes.value.map(n => n.id); 
             console.log("Algorithm Result:", resultData);
        }

        // Notify parent UI
        emit('algorithm-complete', {
            algo: algo,
            time: response.data.executionTimeMs,
            result: resultData
        });

    } catch (e) {
        console.error("Algo Failed", e);
        pathNodeIds.value = [];
        lastExecutionTime.value = null;
        alert("Algorithm failed or encountered an error (Check console).");
    }
};

const clearGraph = async () => {
    nodes.value = [];
    edges.value = [];
    pathNodeIds.value = [];
    selectedNodeId.value = null;
    lastExecutionTime.value = null;
    await createGraph();
};

const scatterNodes = (serverNodes) => {
    const positions = [];
    const minDistance = 45; // Prevent collision explicitly
    const maxAttempts = 500;
    
    // Estimate bounds safely if canvas isn't fully drawn yet
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
                if (Math.sqrt(dx*dx + dy*dy) < minDistance) {
                    validPosition = false;
                    break;
                }
            }
            attempt++;
        }
        positions.push({x, y});
        
        return {
            id: n.id,
            label: n.id.substring(0,4),
            x: x,
            y: y
        };
    });
};

const seedRandomGraph = async () => {
    try {
        const response = await axios.post(`${apiBase}/seed/random?nodes=30&edges=60`);
        graphId.value = response.data.id;
        console.log("Seeded Graph ID:", graphId.value);
        
        // Fetch nodes and edges to render them
        const nodesReq = await axios.get(`${apiBase}/${graphId.value}/nodes`);
        const edgesReq = await axios.get(`${apiBase}/${graphId.value}/edges`);
        
        // Use scatter logic to prevent overlapping nodes
        nodes.value = scatterNodes(nodesReq.data);
        
        edges.value = edgesReq.data.map(e => ({
            source: e.sourceId,
            target: e.targetId,
            weight: e.weight
        }));
        
        pathNodeIds.value = [];
        selectedNodeId.value = null;
        lastExecutionTime.value = null;
        
        emit('graph-updated', nodes.value);

    } catch (e) {
        console.error("Failed to seed graph", e);
        alert("Failed to generate random graph. Is backend running?");
    }
}

const loadGraphId = async (id) => {
    graphId.value = id;
    try {
        const nodesReq = await axios.get(`${apiBase}/${graphId.value}/nodes`);
        const edgesReq = await axios.get(`${apiBase}/${graphId.value}/edges`);
        
        // Use scatter logic
        nodes.value = scatterNodes(nodesReq.data);
        
        edges.value = edgesReq.data.map(e => ({
            source: e.sourceId, target: e.targetId, weight: e.weight
        }));
        
        emit('graph-updated', nodes.value);
        pathNodeIds.value = []; selectedNodeId.value = null; lastExecutionTime.value = null;
    } catch (e) {
        console.error("Load failed", e);
    }
}

defineExpose({ runAlgorithm, clearGraph, seedRandomGraph, loadGraphId, lastExecutionTime, graphId });
</script>

<template>
    <div class="canvas-container">
        <canvas ref="canvas" @click="handleCanvasClick"></canvas>
        <div class="instructions glass-panel">
            <p><strong>Click empty space</strong> to create nodes.</p>
            <p><strong>Click a node, then another</strong> to create an edge.</p>
            <p>Pathfinding runs from Node 1 to the most recently created Node.</p>
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
    font-size: 0.9rem;
    color: #cbd5e1;
}
</style>
