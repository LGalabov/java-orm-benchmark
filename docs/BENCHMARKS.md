# Benchmark Specification

What each test produces, what the three output reports contain, and how results map to the value propositions in the README.

## The Three Reports

The PDF README promises three outputs per run. This document specifies exactly what goes into each one.

### Report 1: Performance Summary

**Purpose**: The raw comparison. Latency and throughput per subject per query type.

**Contents**:

|Metric     |Unit |Source        |Per             |
|-----------|-----|--------------|----------------|
|Latency p50|μs   |JMH SampleTime|subject × query |
|Latency p95|μs   |JMH SampleTime|subject × query |
|Latency p99|μs   |JMH SampleTime|subject × query |
|Throughput |ops/s|JMH Throughput|subject × query |

**Queries covered**: All 10 from QUERIES.md (Q1–Q10). Database-specific features (CTE, JSONB, FTS, window functions) are tracked in the Feature Matrix but not benchmarked for latency.

**JSON output**: `results/round-NNN/latency.json` and `results/round-NNN/throughput.json`.

-----

### Report 2: Overhead Analysis

**Purpose**: The cost breakdown that frameworks do not publish. Four dimensions.

#### 2a: Call Stack Depth

**What**: Frames from user's adapter method to JDBC `execute()`.

**How**: Separate measurement pass. `Thread.getStackTrace()` at the JDBC driver boundary. Count frames between adapter method and `PreparedStatement.execute()`.

**Output**:

```json
{
  "subject": "hibernate",
  "query": "pk_lookup",
  "stack_depth": 34,
  "notable_frames": ["SessionImpl.find", "DefaultLoadEventListener.onLoad", "EntityLoader.load"]
}
```

#### 2b: Heap Allocations per Query

**What**: Bytes and objects per query execution.

**How**: JMH GC profiler (`-prof gc`). Reports `gc.alloc.rate.norm` (bytes/op).

**Output**:

```json
{ "subject": "hibernate", "alloc_bytes_per_op": 4820, "alloc_objects_per_op": 58 }
```

#### 2c: GC Pressure Under Sustained Load

**What**: GC pauses during 60-second sustained load at 1000 ops/sec.

**How**: Custom runner monitoring `GarbageCollectorMXBean`.

**Output**:

```json
{
  "subject": "hibernate",
  "sustained_load": {
    "target_ops_sec": 1000, "duration_sec": 60, "actual_ops": 59847,
    "gc_pauses": 12, "gc_total_ms": 45, "gc_max_pause_ms": 8
  }
}
```

#### 2d: Cold Start Time

**What**: Wall clock from `adapter.setup()` to first successful query result.

**How**: `System.nanoTime()` before setup, after first findUserById returns. Run 5 times, take median.

**Output**:

```json
{
  "subject": "hibernate",
  "cold_start_ms": 2340,
  "breakdown": { "framework_init_ms": 1850, "first_query_ms": 490 }
}
```

-----

### Report 3: Crossover Report

**Purpose**: At what point does each framework's overhead become the dominant cost — or become irrelevant?

#### 3a: ORM Overhead vs. Network Latency

**Formula**: `negligible_rtt = overhead_us / 0.05`

**Output**:

```json
{
  "subject": "hibernate",
  "pk_lookup_overhead_us": 82,
  "negligible_when_rtt_gt_ms": 1.64,
  "verdict": "Overhead dominates on local databases. Negligible over network."
}
```

#### 3b: Batch Size Inflection Points

**Output**:

```json
{
  "subject": "hibernate",
  "batch_insert": {
    "individual_us": 125,
    "batch_100_us_per_row": 24.5,
    "batch_1k_us_per_row": 23.2,
    "batch_10k_us_per_row": 22.8,
    "break_even_rows": 12
  }
}
```

#### 3c: Query Complexity Scaling

**Tiers**: PK Lookup → Filter+Sort+Paginate → 3-Table Join → Aggregation.

**Output**:

```json
{
  "subject": "hibernate",
  "complexity_scaling": {
    "pk_lookup_overhead_us": 82,
    "filter_sort_paginate_overhead_us": 108,
    "join_3table_overhead_us": 190,
    "aggregation_overhead_us": 195,
    "pattern": "Scales with result mapping complexity, not query complexity"
  }
}
```

#### 3d: Concurrency Sweep

**What**: Throughput at thread counts 1, 4, 8, 16, 32 with fixed pool size (10).

**Output**:

```json
{
  "subject": "hibernate",
  "concurrency_sweep": [
    { "threads": 1, "throughput_ops_s": 8500 },
    { "threads": 4, "throughput_ops_s": 28000 },
    { "threads": 8, "throughput_ops_s": 42000 },
    { "threads": 16, "throughput_ops_s": 38000 },
    { "threads": 32, "throughput_ops_s": 35000 }
  ],
  "saturation_point": 8,
  "pool_size": 10
}
```
