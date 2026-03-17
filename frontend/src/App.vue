<script setup>
import { ref, computed } from 'vue';
import axios from 'axios';
import GraphCanvas from './components/GraphCanvas.vue';

const graphCanvasRef = ref(null);
const currentAlgo = ref('dijkstra');

// State elevated from Canvas for UI Controls
const availableNodes = ref([]);
const sourceNodeId = ref('');
const targetNodeId = ref('');

// State for Results Panel
const lastExecutionTime = ref(null);
const lastAlgoResult = ref(null);
const lastDistance = ref(null);
const algoError = ref(null);

// Loading & modal state
const isLoading = ref(false);
const showModal = ref(false);
const modalTitle = ref('');
const modalPlaceholder = ref('');
const modalInput = ref('');
const modalResolve = ref(null);

// Open a modal and return a promise with the user's input
const openModal = (title, placeholder, defaultVal = '') => {
    return new Promise((resolve) => {
        modalTitle.value = title;
        modalPlaceholder.value = placeholder;
        modalInput.value = defaultVal;
        modalResolve.value = resolve;
        showModal.value = true;
    });
};

const confirmModal = () => {
    showModal.value = false;
    if (modalResolve.value) modalResolve.value(modalInput.value.trim());
    modalResolve.value = null;
};

const cancelModal = () => {
    showModal.value = false;
    if (modalResolve.value) modalResolve.value(null);
    modalResolve.value = null;
};

const handleClear = () => {
    if (graphCanvasRef.value) {
        graphCanvasRef.value.clearGraph();
        resetResults();
    }
}

const handleRun = () => {
    if (graphCanvasRef.value) {
        graphCanvasRef.value.runAlgorithm(currentAlgo.value, sourceNodeId.value, targetNodeId.value);
    }
}

const handleSeed = () => {
    if (graphCanvasRef.value) {
        graphCanvasRef.value.seedRandomGraph();
        resetResults();
    }
}

const handleResetView = () => {
    if (graphCanvasRef.value) {
        graphCanvasRef.value.resetView();
    }
}

// Emitted from GraphCanvas when data changes
const onGraphUpdated = (nodes) => {
    availableNodes.value = nodes;
    if (nodes.length > 0) {
        if (!sourceNodeId.value) sourceNodeId.value = nodes[0].id;
        if (!targetNodeId.value) targetNodeId.value = nodes[Math.min(1, nodes.length - 1)].id;
    }
}

const onAlgorithmComplete = (payload) => {
    if (payload.error) {
        algoError.value = payload.error;
        lastExecutionTime.value = null;
        lastAlgoResult.value = null;
        lastDistance.value = null;
        return;
    }
    algoError.value = null;
    lastExecutionTime.value = payload.time;
    lastAlgoResult.value = payload.result;

    if (payload.algo === 'dijkstra' || payload.algo === 'astar') {
        lastDistance.value = payload.result?.totalCost ?? null;
    } else {
        lastDistance.value = null;
    }
}

const onLoading = (loading) => {
    isLoading.value = loading;
}

const resetResults = () => {
    lastExecutionTime.value = null;
    lastAlgoResult.value = null;
    lastDistance.value = null;
    algoError.value = null;
}

const sortedAlgoResult = computed(() => {
    if (!lastAlgoResult.value) return [];

    return Object.entries(lastAlgoResult.value).sort((a, b) => {
        const valA = parseFloat(a[1]);
        const valB = parseFloat(b[1]);
        if (valA > valB) return -1;
        if (valA < valB) return 1;
        return 0;
    });
});

// Persistence
const apiBase = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api/graphs';

const handleSave = async () => {
    if (!graphCanvasRef.value?.graphId) return;

    const title = await openModal('Save Graph', 'Enter a filename (e.g., network1)', 'network1');
    if (!title) return;

    isLoading.value = true;
    try {
        const filename = encodeURIComponent(title + '.json');
        await axios.post(`${apiBase}/${graphCanvasRef.value.graphId}/save?filename=${filename}`);
        algoError.value = null;
    } catch (e) {
        algoError.value = 'Failed to save: ' + (e.response?.data || 'Unknown error');
    } finally {
        isLoading.value = false;
    }
}

const handleLoad = async () => {
    if (!graphCanvasRef.value) return;

    const title = await openModal('Load Graph', 'Enter the filename to load (e.g., network1)', 'network1');
    if (!title) return;

    isLoading.value = true;
    try {
        const filename = encodeURIComponent(title + '.json');
        const res = await axios.post(`${apiBase}/load?filename=${filename}`);
        graphCanvasRef.value.loadGraphId(res.data.id);
        resetResults();
    } catch (e) {
        algoError.value = 'Failed to load: ' + (e.response?.data || 'File not found');
    } finally {
        isLoading.value = false;
    }
}
</script>

<template>
  <div class="app-container">
    <!-- Loading overlay -->
    <Transition name="fade">
      <div v-if="isLoading" class="loading-overlay">
        <div class="spinner"></div>
        <p>Processing...</p>
      </div>
    </Transition>

    <!-- Modal -->
    <Transition name="fade">
      <div v-if="showModal" class="modal-overlay" @click.self="cancelModal">
        <div class="modal glass-panel">
          <h3>{{ modalTitle }}</h3>
          <input
            v-model="modalInput"
            :placeholder="modalPlaceholder"
            class="modal-input"
            @keyup.enter="confirmModal"
            ref="modalInputRef"
            autofocus
          />
          <div class="modal-actions">
            <button class="secondary-btn" @click="cancelModal">Cancel</button>
            <button class="run-btn" @click="confirmModal" :disabled="!modalInput.trim()">Confirm</button>
          </div>
        </div>
      </div>
    </Transition>

    <header class="topbar glass-panel">
      <div>
        <h1>Graph Analytics Engine</h1>
        <p>Interactive Visualization Layer</p>
      </div>
      <div class="author-credits">
        Made by <strong>Mehdi Lakhouane</strong>
      </div>
    </header>

    <div class="main-content">
      <!-- LEFT COLUMN: CONTROLS -->
      <aside class="sidebar-left glass-panel">
        <h2>Controls</h2>

        <div class="control-group">
          <label>Algorithm Selection</label>
          <select v-model="currentAlgo">
            <option value="dijkstra">Dijkstra (Shortest Path)</option>
            <option value="astar">A* (Heuristic Shortest Path)</option>
            <option value="bfs">Breadth-First Search</option>
            <option value="dfs">Depth-First Search</option>
            <option value="pagerank">PageRank</option>
            <option value="louvain">Louvain Community Detection</option>
            <option value="closeness">Closeness Centrality</option>
            <option value="betweenness">Betweenness Centrality</option>
          </select>
        </div>

        <div class="control-group" v-if="['dijkstra', 'astar', 'bfs', 'dfs'].includes(currentAlgo)">
          <label>Source Node</label>
          <select v-model="sourceNodeId">
             <option v-for="n in availableNodes" :key="n.id" :value="n.id">{{ n.label }}</option>
          </select>
        </div>

        <div class="control-group" v-if="['dijkstra', 'astar'].includes(currentAlgo)">
          <label>Target Node</label>
          <select v-model="targetNodeId">
             <option v-for="n in availableNodes" :key="n.id" :value="n.id">{{ n.label }}</option>
          </select>
        </div>

        <div class="actions">
          <button @click="handleRun" class="run-btn" :disabled="isLoading">
            {{ isLoading ? 'Running...' : 'Run Algorithm' }}
          </button>
          <button @click="handleSeed" class="seed-btn" :disabled="isLoading">Generate Random Graph</button>
          <button @click="handleResetView" class="secondary-btn">Reset View</button>
          <button @click="handleClear" class="clear-btn" :disabled="isLoading">Clear Canvas</button>
        </div>

        <div class="api-actions">
           <button @click="handleSave" class="secondary-btn" :disabled="isLoading">Save Graph</button>
           <button @click="handleLoad" class="secondary-btn" :disabled="isLoading">Load Graph</button>
        </div>
      </aside>

      <!-- CENTER COLUMN: CANVAS -->
      <main class="canvas-area">
        <GraphCanvas
          ref="graphCanvasRef"
          @graph-updated="onGraphUpdated"
          @algorithm-complete="onAlgorithmComplete"
          @loading="onLoading"
        />
      </main>

      <!-- RIGHT COLUMN: RESULTS -->
      <aside class="sidebar-right glass-panel">
        <h2>Results Panel</h2>

        <!-- Error state -->
        <div v-if="algoError" class="error-state">
          <p>{{ algoError }}</p>
        </div>

        <div v-else-if="lastExecutionTime === null" class="empty-state">
           <p>Run an algorithm to see metrics here.</p>
        </div>
        <div v-else class="results-content">
            <div class="metric-box">
                <span class="metric-label">Execution Time</span>
                <span class="metric-value highlight">{{ lastExecutionTime }} ms</span>
            </div>

            <div v-if="lastDistance !== null" class="metric-box">
                <span class="metric-label">Total Path Cost</span>
                <span class="metric-value highlight">{{ lastDistance.toFixed(2) }}</span>
            </div>

            <!-- Path Visualization -->
            <div v-if="['dijkstra', 'astar'].includes(currentAlgo) && lastAlgoResult?.pathNodeIds" class="path-results">
                <h3>Optimal Path</h3>
                <div class="path-sequence">
                    <span v-for="(node, idx) in lastAlgoResult.pathNodeIds" :key="idx" class="path-node">
                        {{ availableNodes.find(n => n.id === node)?.label || 'Node' }}
                        <span v-if="idx < lastAlgoResult.pathNodeIds.length - 1" class="path-arrow">&rarr;</span>
                    </span>
                </div>
            </div>

            <!-- Map Visualizations (PageRank, Louvain, Centrality) -->
            <div v-if="['pagerank', 'louvain', 'closeness', 'betweenness'].includes(currentAlgo) && lastAlgoResult" class="map-results">
                <h3>Network Metrics</h3>
                <div class="metric-scrollbox">
                   <div v-for="[key, val] in sortedAlgoResult" :key="key" class="metric-row">
                      <span class="node-id">{{ availableNodes.find(n => n.id === key)?.label || key.substring(0,6) }}</span>
                      <span class="node-val">{{ typeof val === 'number' ? val.toFixed(4) : val }}</span>
                   </div>
                </div>
            </div>

             <!-- Traversal Visualizations (BFS, DFS) -->
            <div v-if="['bfs', 'dfs'].includes(currentAlgo) && lastAlgoResult" class="path-results">
                <h3>Traversal Order (First 20)</h3>
                <div class="path-sequence">
                    <span v-for="(node, idx) in lastAlgoResult.slice(0, 20)" :key="idx" class="path-node">
                        {{ availableNodes.find(n => n.id === node)?.label || 'Node' }}
                        <span v-if="idx < Math.min(lastAlgoResult.length, 20) - 1" class="path-arrow">&rarr;</span>
                    </span>
                    <span v-if="lastAlgoResult.length > 20" class="path-node">... ({{ lastAlgoResult.length - 20 }} more)</span>
                </div>
            </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.app-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 100vw;
  padding: 1rem;
  box-sizing: border-box;
  gap: 1rem;
}

/* Loading Overlay */
.loading-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.7);
  backdrop-filter: blur(4px);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  color: white;
  gap: 1rem;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255,255,255,0.2);
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1001;
}

.modal {
  min-width: 380px;
  max-width: 450px;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.modal h3 {
  margin: 0;
  font-size: 1.15rem;
}

.modal-input {
  padding: 0.75rem;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid var(--border-color);
  color: white;
  font-family: inherit;
  font-size: 1rem;
  outline: none;
  transition: border-color 0.2s;
}

.modal-input:focus {
  border-color: var(--primary-color);
}

.modal-actions {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
}

/* Transitions */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}

.topbar {
  padding: 1rem 2rem;
  text-align: left;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.topbar h1 {
  margin: 0;
  font-size: 1.5rem;
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.topbar p {
  margin: 0;
  color: var(--edge-color);
}

.author-credits {
  font-size: 0.9rem;
  color: var(--text-color);
  opacity: 0.8;
  background: rgba(255, 255, 255, 0.05);
  padding: 0.5rem 1rem;
  border-radius: 20px;
  border: 1px solid var(--border-color);
}
.author-credits strong {
  color: var(--primary-color);
}

.main-content {
  display: flex;
  flex: 1;
  gap: 1rem;
  min-height: 0;
}

.sidebar-left, .sidebar-right {
  width: 320px;
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  overflow-y: auto;
}

h2 {
  margin: 0;
  font-size: 1.25rem;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 0.5rem;
}

.control-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  text-align: left;
}

select {
  padding: 0.75rem;
  border-radius: 8px;
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid var(--border-color);
  color: white;
  font-family: inherit;
  cursor: pointer;
  outline: none;
  transition: all 0.2s;
}

select:focus {
  border-color: var(--primary-color);
}

.actions, .api-actions {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.api-actions {
  border-top: 1px solid var(--border-color);
  padding-top: 1rem;
}

.run-btn { background: #10b981; }
.run-btn:hover:not(:disabled) { background: #059669; box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4); }

.clear-btn { background: transparent; border: 1px solid #ef4444; color: #ef4444; }
.clear-btn:hover:not(:disabled) { background: rgba(239, 68, 68, 0.1); }

.seed-btn { background: #8b5cf6; }
.seed-btn:hover:not(:disabled) { background: #7c3aed; box-shadow: 0 4px 12px rgba(139, 92, 246, 0.4); }

.secondary-btn { background: #334155; }
.secondary-btn:hover:not(:disabled) { background: #475569; }

.canvas-area {
  flex: 1;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.2);
}

/* Results Panel Styling */
.empty-state {
  color: #94a3b8;
  text-align: center;
  margin-top: 2rem;
  font-style: italic;
}

.error-state {
  color: #ef4444;
  text-align: center;
  margin-top: 1rem;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 8px;
  padding: 1rem;
}

.results-content {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.metric-box {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255,255,255,0.05);
  padding: 1rem;
  border-radius: 12px;
  border: 1px solid var(--border-color);
}

.metric-label {
  color: #cbd5e1;
  font-size: 0.9rem;
}

.metric-value.highlight {
  font-size: 1.1rem;
  font-weight: 600;
  color: #38bdf8;
}

.path-results h3 {
  font-size: 1rem;
  margin: 0 0 0.5rem 0;
  color: #f8fafc;
}

.path-sequence {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  background: rgba(0,0,0,0.2);
  padding: 1rem;
  border-radius: 8px;
}

.path-node {
  font-size: 0.9rem;
  color: #ef4444;
  font-weight: 600;
}

.path-arrow {
  color: #64748b;
  margin-left: 0.5rem;
}

.metric-scrollbox {
  max-height: 400px;
  overflow-y: auto;
  background: rgba(0,0,0,0.2);
  border-radius: 8px;
  padding: 0.5rem;
}

.metric-row {
  display: flex;
  justify-content: space-between;
  padding: 0.5rem;
  border-bottom: 1px solid rgba(255,255,255,0.05);
  font-family: monospace;
}

.node-id { color: #94a3b8; }
.node-val { color: #10b981; font-weight: bold; }
</style>
