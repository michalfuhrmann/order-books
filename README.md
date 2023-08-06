# Order Books

Trying to find the best implementation of an order book

Conclusions so far:

- it seems ArrayDeque is only slightly faster than LinkedList implementation ( maybe due to dataset, probably should be more complex)
- ListBased implementation that uses offsets to calculate position in the list based on the price is also slightly slower when it comes to addding
  elements
- Based on profiling it seems most of them is anyway spend during construction of time objects, so the implementations does not differ that much
- Base on JMH data we are peaking with 25m inserts/sec right now

### RoadMap

- mutable and immutable orders comparison
- Fix ListBasedOrderBook implementation, tests pass but JMH fails so definitely some coverage is missing.
- Better data distribution

# Results so far from JMH

| Benchmark                                                      | Mode  | Cnt | Score   | Error    | Units  |
|----------------------------------------------------------------|-------|-----|---------|----------|--------|
| OrderBookAddOrdersBenchmark.addOrders_basicOrderBook           | thrpt | 3   | 25,658  | ± 8,628  | ops/s  |
| OrderBookAddOrdersBenchmark.addOrders_basicOrderBookArrayDeque | thrpt | 3   | 24,827  | ± 0,616  | ops/s  |
| OrderBookAddOrdersBenchmark.addOrders_listBasedOrderBook       | thrpt | 3   | 24,600  | ± 5,671  | ops/s  |
| OrderBookAddOrdersBenchmark.basicOrder_LevelStacking           | thrpt | 3   | 10,190  | ± 2,324  | ops/s  |
| OrderBookAddOrdersBenchmark.basicOrder_LevelStacking_V2        | thrpt | 3   | 10,408  | ± 0,754  | ops/s  |
| ---                                                            | ---   | --- | ---     | ---      | ---    |
| OrderBookTimeSource.fakeTimeSource                             | thrpt | 3   | 184,061 | ± 12,667 | ops/us |
| OrderBookTimeSource.instant                                    | thrpt | 3   | 70,764  | ± 1,939  | ops/us |
| OrderBookTimeSource.nanoTime                                   | thrpt | 3   | 51,296  | ± 1,421  | ops/us |
| OrderBookTimeSource.tickClock                                  | thrpt | 3   | 67,517  | ± 3,659  | ops/us |
| OrderBookTimeSource.timeMillis                                 | thrpt | 3   | 350,071 | ± 48,198 | ops/us |
