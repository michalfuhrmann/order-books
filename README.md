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
-  
