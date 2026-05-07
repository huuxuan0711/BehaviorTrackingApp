package com.xmobile.project2digitalwellbeing.presentation.analysis.transition

import org.json.JSONArray
import org.json.JSONObject

object AppTransitionGraphHtmlRenderer {

    fun render(
        nodes: List<GraphNodeUiModel>,
        edges: List<GraphEdgeUiModel>,
        iconDataByNodeId: Map<String, String>
    ): String {
        val nodesJson = JSONArray().apply {
            nodes.forEach { node ->
                put(
                    JSONObject().apply {
                        put("id", node.id)
                        put("label", node.label)
                        put("icon", iconDataByNodeId[node.id] ?: "")
                    }
                )
            }
        }
        val edgesJson = JSONArray().apply {
            edges.forEach { edge ->
                put(
                    JSONObject().apply {
                        put("from", edge.fromId)
                        put("to", edge.toId)
                        put("count", edge.count)
                        put("frequent", edge.frequent)
                    }
                )
            }
        }

        return """
            <!doctype html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1" />
              <style>
                body { margin: 0; background: #ffffff; font-family: sans-serif; }
                #root { width: 100%; height: 400px; position: relative; }
                svg { width: 100%; height: 100%; display: block; }
                .node text { fill: #1f2937; font-size: 12px; text-anchor: middle; }
                .node .halo { fill: #36a74a; opacity: 0; }
                .node circle { fill: #f7f9ff; stroke: #5761c9; stroke-width: 2; }
                .node.selected .halo { opacity: 0.18; }
                .node.selected circle { stroke: #36a74a; stroke-width: 3; }
                .edge { fill: none; stroke-linecap: round; marker-end: url(#arrow); opacity: 0.95; }
                .edge.frequent { stroke: #5761c9; }
                .edge.occasional { stroke: #b8b8b8; }
                .muted { opacity: 0.16 !important; }
              </style>
            </head>
            <body>
              <div id="root"></div>
              <script>
                const DATA = { nodes: $nodesJson, edges: $edgesJson };
                const W = 360, H = 400;
                const root = document.getElementById('root');
                const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
                svg.setAttribute('viewBox', '0 0 ' + W + ' ' + H);
                root.appendChild(svg);

                const defs = document.createElementNS('http://www.w3.org/2000/svg', 'defs');
                defs.innerHTML = `
                  <marker id="arrow" markerWidth="4.5" markerHeight="4.5" refX="4.1" refY="2.25" orient="auto">
                    <path d="M0,0 L4.5,2.25 L0,4.5 Z" fill="#6670d8"></path>
                  </marker>
                `;
                svg.appendChild(defs);

                if (!DATA.nodes.length) {
                  const text = document.createElementNS('http://www.w3.org/2000/svg', 'text');
                  text.setAttribute('x', W / 2);
                  text.setAttribute('y', H / 2);
                  text.setAttribute('text-anchor', 'middle');
                  text.setAttribute('fill', '#9ca3af');
                  text.textContent = 'No transition data';
                  svg.appendChild(text);
                } else {
                  const positions = {};
                  const slotsByCount = {
                    1: [[180, 115]],
                    2: [[180, 90], [180, 230]],
                    3: [[180, 85], [100, 210], [260, 210]],
                    4: [[180, 80], [85, 170], [275, 170], [180, 265]],
                    5: [[180, 75], [80, 155], [280, 155], [115, 265], [245, 265]],
                    6: [[180, 70], [70, 145], [290, 145], [85, 250], [275, 250], [180, 305]],
                    7: [[180, 68], [68, 136], [292, 136], [70, 238], [290, 238], [126, 308], [234, 308]]
                  };
                  const slots = slotsByCount[Math.min(7, DATA.nodes.length)] || slotsByCount[7];
                  DATA.nodes.forEach((node, index) => {
                    const slot = slots[index] || slots[slots.length - 1];
                    positions[node.id] = { x: slot[0], y: slot[1] };
                  });

                  const maxCount = DATA.edges.reduce((m, e) => Math.max(m, e.count), 1);
                  const edgeEls = [];
                  const nodeRadius = 25;
                  DATA.edges.forEach((edge) => {
                    const from = positions[edge.from], to = positions[edge.to];
                    if (!from || !to) return;
                    const dx = to.x - from.x, dy = to.y - from.y;
                    const baseLen = Math.max(1, Math.hypot(dx, dy));
                    const ux = dx / baseLen, uy = dy / baseLen;
                    const startInset = nodeRadius + 2;
                    const endInset = nodeRadius + 10;
                    const sx = from.x + ux * startInset;
                    const sy = from.y + uy * startInset;
                    const tx = to.x - ux * endInset;
                    const ty = to.y - uy * endInset;
                    const midX = (sx + tx) / 2;
                    const midY = (sy + ty) / 2;
                    const nx = -dy, ny = dx;
                    const len = Math.max(1, Math.hypot(nx, ny));
                    const curve = 24;
                    const cx1 = midX + (nx / len) * curve;
                    const cy1 = midY + (ny / len) * curve;
                    const thickness = 1.4 + (edge.count / maxCount) * 4;

                    const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
                    path.setAttribute('d', 'M ' + sx + ' ' + sy + ' Q ' + cx1 + ' ' + cy1 + ' ' + tx + ' ' + ty);
                    path.setAttribute('class', 'edge ' + (edge.frequent ? 'frequent' : 'occasional'));
                    path.setAttribute('stroke-width', thickness.toFixed(2));
                    path.dataset.from = edge.from;
                    path.dataset.to = edge.to;
                    svg.appendChild(path);
                    edgeEls.push(path);
                  });

                  const nodeEls = [];
                  DATA.nodes.forEach((node) => {
                    const p = positions[node.id];
                    const g = document.createElementNS('http://www.w3.org/2000/svg', 'g');
                    g.setAttribute('class', 'node');
                    g.dataset.id = node.id;
                    g.style.cursor = 'pointer';

                    const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
                    circle.setAttribute('cx', p.x);
                    circle.setAttribute('cy', p.y);
                    circle.setAttribute('r', 25);
                    const halo = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
                    halo.setAttribute('cx', p.x);
                    halo.setAttribute('cy', p.y);
                    halo.setAttribute('r', 31);
                    halo.setAttribute('class', 'halo');
                    g.appendChild(halo);
                    g.appendChild(circle);

                    const iconUrl = (node.icon || '').trim();
                    if (iconUrl.length > 0) {
                      const image = document.createElementNS('http://www.w3.org/2000/svg', 'image');
                      image.setAttribute('x', p.x - 11);
                      image.setAttribute('y', p.y - 11);
                      image.setAttribute('width', 22);
                      image.setAttribute('height', 22);
                      image.setAttribute('href', iconUrl);
                      g.appendChild(image);
                    } else {
                      const icon = document.createElementNS('http://www.w3.org/2000/svg', 'text');
                      icon.setAttribute('x', p.x);
                      icon.setAttribute('y', p.y + 4);
                      icon.setAttribute('fill', '#6b7280');
                      icon.textContent = (node.label || '?').trim().slice(0, 1).toUpperCase();
                      g.appendChild(icon);
                    }

                    const label = document.createElementNS('http://www.w3.org/2000/svg', 'text');
                    label.setAttribute('x', p.x);
                    label.setAttribute('y', p.y + 40);
                    label.textContent = node.label;

                    g.appendChild(label);
                    svg.appendChild(g);
                    nodeEls.push(g);
                  });

                  let selected = null;
                  const applySelection = () => {
                    nodeEls.forEach((n) => n.classList.remove('selected', 'muted'));
                    edgeEls.forEach((e) => e.classList.remove('muted'));
                    if (!selected) return;
                    const connectedNodeIds = new Set([selected]);
                    edgeEls.forEach((e) => {
                      if (e.dataset.from === selected) connectedNodeIds.add(e.dataset.to);
                      if (e.dataset.to === selected) connectedNodeIds.add(e.dataset.from);
                    });
                    nodeEls.forEach((n) => {
                      if (n.dataset.id === selected) {
                        n.classList.add('selected');
                      } else if (!connectedNodeIds.has(n.dataset.id)) {
                        n.classList.add('muted');
                      }
                    });
                    edgeEls.forEach((e) => {
                      if (e.dataset.from === selected || e.dataset.to === selected) return;
                      e.classList.add('muted');
                    });
                  };

                  nodeEls.forEach((nodeEl) => {
                    nodeEl.addEventListener('click', () => {
                      selected = (selected === nodeEl.dataset.id) ? null : nodeEl.dataset.id;
                      applySelection();
                    });
                  });
                }
              </script>
            </body>
            </html>
        """.trimIndent()
    }
}
