<script setup>
import { ref } from 'vue';
import GraphCanvas from './components/GraphCanvas.vue';

const graphCanvasRef = ref(null);
const currentAlgo = ref('dijkstra');

const handleClear = () => {
  if(graphCanvasRef.value) {
    graphCanvasRef.value.clearGraph();
  }
}

const handleRun = () => {
  if(graphCanvasRef.value) {
    graphCanvasRef.value.runAlgorithm(currentAlgo.value);
  }
}
</script>

<template>
  <div class="app-container">
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
      <aside class="sidebar glass-panel">
        <h2>Controls</h2>
        
        <div class="control-group">
          <label>Algorithm Selection</label>
          <select v-model="currentAlgo">
            <option value="dijkstra">Dijkstra (Shortest Path)</option>
            <option value="astar">A* (Heuristic Shortest Path)</option>
          </select>
        </div>
        
        <div class="actions">
          <button @click="handleRun" class="run-btn">Run Search</button>
          <button @click="handleClear" class="clear-btn">Clear Canvas</button>
        </div>
      </aside>
      
      <main class="canvas-area">
        <GraphCanvas ref="graphCanvasRef" />
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  padding: 1rem;
  box-sizing: border-box;
  gap: 1rem;
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

.sidebar {
  width: 300px;
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.sidebar h2 {
  margin: 0 0 1rem 0;
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

.actions {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-top: auto;
}

.run-btn {
  background: #10b981;
}

.run-btn:hover {
  background: #059669;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.4);
}

.clear-btn {
  background: transparent;
  border: 1px solid #ef4444;
  color: #ef4444;
}

.clear-btn:hover {
  background: rgba(239, 68, 68, 0.1);
  box-shadow: none;
}

.canvas-area {
  flex: 1;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 30px rgba(0, 0, 0, 0.2);
}
</style>


