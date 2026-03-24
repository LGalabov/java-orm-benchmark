# Results Site Architecture

The public results live at `https://lgalabov.github.io/java-orm-benchmark/`. The GitHub Gist becomes a redirect.

## Architecture

### Data Layer

Every benchmark run produces a JSON directory. These accumulate in `results/`.

```
results/
├── rounds.json              ← index of all rounds
├── round-001/
│   ├── meta.json            ← environment, JMH params, timestamp
│   ├── latency.json         ← p50/p95/p99 per subject per query
│   ├── throughput.json      ← ops/sec
│   ├── overhead.json        ← stack depth, allocations, GC, cold start
│   └── crossover.json       ← derived analysis
├── round-002/
│   └── ...
```

### `rounds.json` Format

```json
{
  "rounds": [
    {
      "id": "round-001",
      "timestamp": "2026-04-15T14:30:00Z",
      "label": "Round 1 — JDBC + Hibernate",
      "subjects": ["jdbc", "hibernate"],
      "java_version": "25",
      "notes": "Initial baseline."
    }
  ],
  "latest": "round-001"
}
```

### Presentation Layer

Single-page React app (Vite) deployed to GitHub Pages. All data fetched as static JSON.

```
site/
├── src/
│   ├── App.jsx
│   ├── components/
│   │   ├── RoundSelector.jsx
│   │   ├── LatencyTable.jsx
│   │   ├── LatencyChart.jsx
│   │   ├── OverheadPanel.jsx
│   │   ├── CrossoverPanel.jsx
│   │   ├── FeatureMatrix.jsx
│   │   ├── FilterBar.jsx
│   │   └── CompareRounds.jsx
│   └── data/loader.js
├── vite.config.js
└── package.json
```

### Deployment

GitHub Actions on push to main (when site/ or results/ change). Build Vite, copy results/ into dist/, deploy to gh-pages.

## UI Design

**Aesthetic**: Industrial data. Dark background, high-contrast monospace numbers, minimal chrome.

**Core interactions**:

1. Round Selector — dropdown, default "Latest", compare toggle for side-by-side
1. Filter Bar — toggle pills for subjects, percentile (p50/p95/p99). State in URL hash.
1. Latency Table — color-coded cells (green ≤2× baseline, yellow 2–5×, red >5×), optional bar overlay
1. Overhead Panel — horizontal bar charts for stack depth, allocations, cold start
1. Crossover Panel — ORM overhead vs RTT, batch inflection, complexity scaling
1. Feature Matrix — native/passthrough/blocked color-coded table
1. Compare Rounds — two rounds side by side with delta columns

**URL structure**: `/#/round/round-002?subjects=jdbc,hibernate&percentile=p99`

**Data growth**: ~20 KB per round. GitHub Pages 1 GB limit = decades of headroom.
