const canvas = document.getElementById('grid');
const ctx = canvas.getContext('2d');
const tooltip = document.getElementById('tooltip');
const sidebar = document.getElementById('sidebar');
const worldSelect = document.getElementById('worldSelect');

canvas.width = window.innerWidth;
canvas.height = window.innerHeight;

// i was here.

let chunks = {};
let colorThresholds = [];
let selectedChunk = null;
let offsetX = canvas.width / 2;
let offsetY = canvas.height / 2;
const CHUNK_SIZE = 20;

const wsUrl = `ws://${window.location.host}/ws/heatmap`;
const ws = new WebSocket(wsUrl);

ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    if (data.type === 'HEATMAP_UPDATE') {
        if (data.thresholds) colorThresholds = data.thresholds;

        chunks = {};
        data.chunks.forEach(c => {
            chunks[`${c.world},${c.x},${c.z}`] = c;
        });
        drawGrid();
    }
};

function getHeatColor(heat) {
    for (const t of colorThresholds) {
        if (heat >= t.minHeat) return t.color;
    }
    return '#00ff00';
}

function drawGrid() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    ctx.strokeStyle = '#1e1e1e';
    ctx.lineWidth = 1;
    const startX = offsetX % CHUNK_SIZE;
    const startY = offsetY % CHUNK_SIZE;

    for (let x = startX; x < canvas.width; x += CHUNK_SIZE) {
        ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, canvas.height); ctx.stroke();
    }
    for (let y = startY; y < canvas.height; y += CHUNK_SIZE) {
        ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(canvas.width, y); ctx.stroke();
    }

    ctx.strokeStyle = '#444';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(offsetX, 0); ctx.lineTo(offsetX, canvas.height);
    ctx.moveTo(0, offsetY); ctx.lineTo(canvas.width, offsetY);
    ctx.stroke();

    const activeWorld = worldSelect.value;

    for (const key in chunks) {
        const c = chunks[key];
        if (c.world !== activeWorld) continue;

        const px = offsetX + (c.x * CHUNK_SIZE);
        const pz = offsetY + (c.z * CHUNK_SIZE);

        ctx.fillStyle = getHeatColor(c.heat);
        ctx.fillRect(px + 1, pz + 1, CHUNK_SIZE - 2, CHUNK_SIZE - 2);

        if (c.players > 0) {
            ctx.fillStyle = '#60a5fa';
            ctx.fillRect(px + 5, pz + 5, CHUNK_SIZE - 10, CHUNK_SIZE - 10);
        }
    }
}

canvas.addEventListener('mousemove', (e) => {
    const cx = Math.floor((e.clientX - offsetX) / CHUNK_SIZE);
    const cz = Math.floor((e.clientY - offsetY) / CHUNK_SIZE);
    const activeWorld = worldSelect.value;
    const chunk = chunks[`${activeWorld},${cx},${cz}`];

    if (chunk) {
        tooltip.style.display = 'block';
        tooltip.style.left = e.clientX + 15 + 'px';
        tooltip.style.top = e.clientY + 15 + 'px';
        tooltip.innerHTML = `
                <b>Chunk: ${cx}, ${cz}</b><br>
                World: ${chunk.world}<br>
                Heat: ${chunk.heat.toFixed(1)}<br>
                Players: ${chunk.players}<br>
                Mobs: ${chunk.mobs}<br>
                Items: ${chunk.items}<br>
                Tile Entities: ${chunk.tileEntities}
            `;
    } else {
        tooltip.style.display = 'none';
    }
});

canvas.addEventListener('click', (e) => {
    const cx = Math.floor((e.clientX - offsetX) / CHUNK_SIZE);
    const cz = Math.floor((e.clientY - offsetY) / CHUNK_SIZE);
    const activeWorld = worldSelect.value;
    const chunk = chunks[`${activeWorld},${cx},${cz}`];

    if (chunk) {
        selectedChunk = chunk;
        document.getElementById('sidebar-title').innerText = `Manage Chunk: ${cx}, ${cz}`;
        document.getElementById('sidebar-heat').innerText = chunk.heat.toFixed(1);
        sidebar.style.display = 'block';
    }
});

let isDragging = false, lastX, lastY;
canvas.addEventListener('mousedown', e => { if(e.button === 0 || e.button === 2 || e.shiftKey) { isDragging = true; lastX = e.clientX; lastY = e.clientY; }});
canvas.addEventListener('mouseup', () => isDragging = false);
canvas.addEventListener('mousemove', e => {
    if (isDragging) {
        offsetX += (e.clientX - lastX);
        offsetY += (e.clientY - lastY);
        lastX = e.clientX; lastY = e.clientY;
        drawGrid();
    }
});
canvas.oncontextmenu = () => false;

window.onresize = () => {
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
    drawGrid();
};

function closeSidebar() {
    sidebar.style.display = 'none';
    selectedChunk = null;
}

function sendAction(actionType) {
    if (!selectedChunk) return;
    const payload = {
        action: actionType,
        world: selectedChunk.world,
        chunkX: selectedChunk.x,
        chunkZ: selectedChunk.z
    };
    if (actionType === 'TELEPORT') {
        payload.player = document.getElementById('admin-name').value || "Console";
    }
    ws.send(JSON.stringify(payload));
    alert('Action command sent.');
}