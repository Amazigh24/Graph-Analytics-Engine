<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import axios from 'axios';

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
    await createGraph();
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
        
        // Push node to backend is implicit in edge creation, but let's push it with an edge to itself to register it if needed, or backend auto-creates on edge addition!
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

const runAlgorithm = async (algo) => {
    if (!graphId.value || nodes.value.length < 2) return;
    
    const sourceNode = nodes.value[0].id;
    const targetNode = nodes.value[nodes.value.length - 1].id;
    
    try {
        const url = `${apiBase}/${graphId.value}/algorithms/shortest-path/${algo}`;
        const response = await axios.post(url, null, {
            params: { source: sourceNode, target: targetNode }
        });
        pathNodeIds.value = response.data.pathNodeIds;
        console.log("Path:", pathNodeIds.value);
    } catch (e) {
        console.error("Algo Failed", e);
        pathNodeIds.value = [];
        alert("Path not found or error occurred.");
    }
};

const clearGraph = async () => {
    nodes.value = [];
    edges.value = [];
    pathNodeIds.value = [];
    selectedNodeId.value = null;
    await createGraph();
};

defineExpose({ runAlgorithm, clearGraph });
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
