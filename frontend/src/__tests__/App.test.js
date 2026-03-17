import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import App from '../App.vue';

// Mock axios
vi.mock('axios', () => ({
    default: {
        post: vi.fn(),
        get: vi.fn(),
    }
}));

// Mock GraphCanvas since it relies on canvas APIs
const GraphCanvasStub = {
    template: '<div class="graph-canvas-stub"></div>',
    methods: {
        runAlgorithm: vi.fn(),
        clearGraph: vi.fn(),
        seedRandomGraph: vi.fn(),
        loadGraphId: vi.fn(),
        resetView: vi.fn(),
    },
    setup() {
        return {
            graphId: 'test-graph-id',
            lastExecutionTime: null,
        };
    }
};

describe('App.vue', () => {
    let wrapper;

    beforeEach(() => {
        wrapper = mount(App, {
            global: {
                stubs: {
                    GraphCanvas: GraphCanvasStub
                }
            }
        });
    });

    it('renders the header', () => {
        expect(wrapper.find('h1').text()).toBe('Graph Analytics Engine');
    });

    it('renders all algorithm options', () => {
        const options = wrapper.findAll('select option');
        const firstSelect = wrapper.find('select');
        const algorithmOptions = firstSelect.findAll('option');
        expect(algorithmOptions.length).toBe(8);
        expect(algorithmOptions[0].text()).toContain('Dijkstra');
        expect(algorithmOptions[4].text()).toContain('PageRank');
    });

    it('shows source/target selectors for path algorithms', async () => {
        // Dijkstra is default — should show source AND target
        await wrapper.vm.$nextTick();
        const labels = wrapper.findAll('label');
        const labelTexts = labels.map(l => l.text());
        expect(labelTexts).toContain('Source Node');
        expect(labelTexts).toContain('Target Node');
    });

    it('hides target selector for BFS', async () => {
        const select = wrapper.find('select');
        await select.setValue('bfs');
        await wrapper.vm.$nextTick();

        const labels = wrapper.findAll('label');
        const labelTexts = labels.map(l => l.text());
        expect(labelTexts).toContain('Source Node');
        expect(labelTexts).not.toContain('Target Node');
    });

    it('hides source/target for PageRank', async () => {
        const select = wrapper.find('select');
        await select.setValue('pagerank');
        await wrapper.vm.$nextTick();

        const labels = wrapper.findAll('label');
        const labelTexts = labels.map(l => l.text());
        expect(labelTexts).not.toContain('Source Node');
        expect(labelTexts).not.toContain('Target Node');
    });

    it('shows empty results state by default', () => {
        expect(wrapper.find('.empty-state').exists()).toBe(true);
        expect(wrapper.find('.empty-state').text()).toContain('Run an algorithm');
    });

    it('shows loading overlay when isLoading is true', async () => {
        expect(wrapper.find('.loading-overlay').exists()).toBe(false);

        wrapper.vm.isLoading = true;
        await wrapper.vm.$nextTick();

        expect(wrapper.find('.loading-overlay').exists()).toBe(true);
    });

    it('shows error state when algoError is set', async () => {
        wrapper.vm.algoError = 'Test error message';
        await wrapper.vm.$nextTick();

        expect(wrapper.find('.error-state').exists()).toBe(true);
        expect(wrapper.find('.error-state').text()).toContain('Test error message');
    });

    it('shows modal when openModal is called', async () => {
        expect(wrapper.find('.modal-overlay').exists()).toBe(false);

        // Open modal (don't await — it returns a promise)
        wrapper.vm.openModal('Test Title', 'test placeholder', 'default');
        await wrapper.vm.$nextTick();

        expect(wrapper.find('.modal-overlay').exists()).toBe(true);
        expect(wrapper.find('.modal h3').text()).toBe('Test Title');
    });

    it('closes modal on cancel', async () => {
        const promise = wrapper.vm.openModal('Test', 'placeholder');
        await wrapper.vm.$nextTick();

        wrapper.vm.cancelModal();
        await wrapper.vm.$nextTick();

        const result = await promise;
        expect(result).toBeNull();
        expect(wrapper.find('.modal-overlay').exists()).toBe(false);
    });

    it('returns input on confirm', async () => {
        const promise = wrapper.vm.openModal('Test', 'placeholder', 'myvalue');
        await wrapper.vm.$nextTick();

        wrapper.vm.confirmModal();
        await wrapper.vm.$nextTick();

        const result = await promise;
        expect(result).toBe('myvalue');
    });

    it('disables buttons when loading', async () => {
        wrapper.vm.isLoading = true;
        await wrapper.vm.$nextTick();

        const runBtn = wrapper.find('.run-btn');
        expect(runBtn.attributes('disabled')).toBeDefined();
        expect(runBtn.text()).toContain('Running...');
    });

    it('handles algorithm completion with results', async () => {
        wrapper.vm.onAlgorithmComplete({
            algo: 'dijkstra',
            time: 42,
            result: { pathNodeIds: ['A', 'B', 'C'], totalCost: 3.5 }
        });
        await wrapper.vm.$nextTick();

        expect(wrapper.vm.lastExecutionTime).toBe(42);
        expect(wrapper.vm.lastDistance).toBe(3.5);
        expect(wrapper.find('.metric-value.highlight').text()).toContain('42');
    });

    it('handles algorithm completion with error', async () => {
        wrapper.vm.onAlgorithmComplete({
            algo: 'dijkstra',
            time: null,
            result: null,
            error: 'Node not found'
        });
        await wrapper.vm.$nextTick();

        expect(wrapper.vm.algoError).toBe('Node not found');
        expect(wrapper.vm.lastExecutionTime).toBeNull();
    });

    it('sorts algorithm results descending', () => {
        wrapper.vm.lastAlgoResult = { A: 0.5, B: 0.8, C: 0.1 };

        const sorted = wrapper.vm.sortedAlgoResult;
        expect(sorted[0][0]).toBe('B');
        expect(sorted[1][0]).toBe('A');
        expect(sorted[2][0]).toBe('C');
    });
});
