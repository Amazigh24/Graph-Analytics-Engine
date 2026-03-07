# Graph Analytics Engine

A high-performance, full-stack **Graph Analytics Engine** built entirely from scratch to ingest, process, and dynamically visualize complex graphs. This system demonstrates advanced knowledge of data structures, algorithm design, and modern web application development. 

**Created by: Mehdi Lakhouane**

---

## Features

### 🧠 Core Graph Algorithms
Implemented using the Strategy Design Pattern for robust and extensible architecture:
- **Dijkstra's Algorithm:** Calculates the shortest path between nodes using an optimized Priority Queue.
- **A\* Search Algorithm:** Enhances pathfinding speed by utilizing Euclidean spatial heuristics based on node coordinates.
- **PageRank:** Evaluates node importance iteratively, correctly handling graph equilibrium and damping factors.
- **Louvain Community Detection:** A fast modularity optimization heuristic that clusters interconnected nodes into identified communities.

### 🏗️ Advanced Data Structures
- Built completely custom `Node`, `Edge`, and `Graph` domain models.
- Utilizes an **Adjacency List** graph representation for highly efficient neighbor lookups during algorithm execution.
- Employs thread-safe Java collections (`ConcurrentHashMap`, `CopyOnWriteArrayList`) to support safe concurrent data traversal and ingestion.

### 💻 Interactive Visualization Layer
- A stunning **Vue 3 + Vite** frontend interface featuring a sleek, dark-mode glassmorphism aesthetic.
- A custom-built HTML5 Interactive Canvas allowing users to manually plot spatial nodes and organically connect paths in real-time.
- Live pathfinding highlights calculate visually across the canvas instantly via asynchronous REST API calls to the backend engine.

---

## Tech Stack
- **Backend:** Java 21, Spring Boot 3.2, Maven
- **Frontend:** Vue 3, Vite, Axios
- **Testing:** JUnit 5, Spring MockMvc (Integration Testing)

---

## Getting Started

### Prerequisites
- **Java 21** or later installed.
- **Node.js** and **npm** installed.

### Run the Backend Engine
1. Navigate to the root directory.
2. Run the Spring Boot application using Maven:
   ```bash
   ./mvnw spring-boot:run
   ```
   The REST API will launch on `http://localhost:8080`.

### Run the Frontend Visualizer
1. Open a new terminal and navigate to the `frontend` directory:
   ```bash
   cd frontend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the Vite development server:
   ```bash
   npm run dev
   ```
   The interactive canvas will launch on `http://localhost:5173`. Open it in your browser to begin drawing graphs!

---

**Author:** Mehdi Lakhouane
**License:** MIT
