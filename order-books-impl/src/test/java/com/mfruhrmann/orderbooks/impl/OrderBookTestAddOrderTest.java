package com.mfruhrmann.orderbooks.impl;


import com.mfruhrmann.orderbooks.api.OrderBook;
import com.mfruhrmann.orderbooks.utils.OrderManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.mfruhrmann.orderbooks.api.OrderBook.OrderType.LIMIT;
import static com.mfruhrmann.orderbooks.api.OrderBook.Side.BUY;
import static com.mfruhrmann.orderbooks.api.OrderBook.Side.SELL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class OrderBookTestAddOrderTest {

    private final OrderManager orderManager = new OrderManager();
    private final TradeRecorder tradeRecorder = new TradeRecorder();

    public static Stream<Arguments> orderBooks() {
        return Stream.of(Arguments.of(new BasicOrderBook()));
    }


    @ParameterizedTest
    @MethodSource("orderBooks")
    void addingOrderShouldReturnOrderId(OrderBook orderBook) {

        //Given
        OrderBook.Order buyOrder = orderManager.createOrder(BUY, LIMIT, 100.0, 1);

        //when
        String orderId = orderBook.addOrder(buyOrder);

        //Then
        assertThat(orderBook.getAllOrders()).hasSize(1);
        assertThat(orderBook.getTopBidAsk().bid()).isEqualTo(100.0);
        assertThat(orderId).isNotNull();
        assertThat(orderBook.getOrder(orderId)).isEqualTo(buyOrder);
    }

    @ParameterizedTest
    @MethodSource("orderBooks")
    void shouldUpdateOrderBookOnBothBidAndAsk(OrderBook orderBook) {

        //Given
        OrderBook.Order buyOrder = orderManager.createOrder(BUY, LIMIT, 100.0, 1);
        OrderBook.Order sellOrder = orderManager.createOrder(SELL, LIMIT, 101.0, 1);

        //when
        orderBook.addOrder(buyOrder);
        orderBook.addOrder(sellOrder);

        //Then
        assertThat(orderBook.getAllOrders()).hasSize(2);
        assertThat(orderBook.getTopBidAsk().bid()).isEqualTo(100.0);
        assertThat(orderBook.getTopBidAsk().ask()).isEqualTo(101.0);
    }

    @ParameterizedTest
    @MethodSource("orderBooks")
    void shouldStackOrdersForTheSamePrice(OrderBook orderBook) {

        //Given
        OrderBook.Order buyOrder = orderManager.createOrder(BUY, LIMIT, 100.0, 1);
        OrderBook.Order buyOrderSamePrice = orderManager.createOrder(BUY, LIMIT, 100.0, 2);

        OrderBook.Order sellOrder = orderManager.createOrder(SELL, LIMIT, 101.0, 2);
        OrderBook.Order sellOrderSamePrice = orderManager.createOrder(SELL, LIMIT, 101.0, 4);
        //when
        orderBook.addOrder(buyOrder);
        orderBook.addOrder(buyOrderSamePrice);

        orderBook.addOrder(sellOrder);
        orderBook.addOrder(sellOrderSamePrice);

        //Then
        assertThat(orderBook.getAllOrders()).hasSize(4);

        assertThat(orderBook.getTopBidAsk().bid()).isEqualTo(100.0);
        assertThat(orderBook.getTopBidAsk().bidSize()).isEqualTo(3);
        assertThat(orderBook.getBidLevels()).containsOnlyKeys(100.0);
        assertThat(orderBook.getBidLevels().get(100.0)).isEqualTo(3);

        assertThat(orderBook.getTopBidAsk().ask()).isEqualTo(101.0);
        assertThat(orderBook.getTopBidAsk().askSize()).isEqualTo(6);
        assertThat(orderBook.getAskLevels()).containsOnlyKeys(101.0);
        assertThat(orderBook.getAskLevels().get(101.0)).isEqualTo(6);
    }

    @ParameterizedTest
    @MethodSource("orderBooks")
    void shouldCreateTradeIfTwoOrdersMatch(OrderBook orderBook) {
        orderBook.addTradeListener(tradeRecorder);

        //Given
        OrderBook.Order buyOrder = orderManager.createOrder(BUY, LIMIT, 100.0, 1);
        OrderBook.Order sellOrder = orderManager.createOrder(SELL, LIMIT, 100.0, 1);
        OrderBook.Order buyOrder2 = orderManager.createOrder(BUY, LIMIT, 100.0, 1);
        OrderBook.Order sellOrder2 = orderManager.createOrder(SELL, LIMIT, 100.0, 1);

        //when
        orderBook.addOrder(buyOrder);
        orderBook.addOrder(sellOrder);

        orderBook.addOrder(sellOrder2);
        orderBook.addOrder(buyOrder2);

        //Then
        assertThat(orderBook.getAllOrders()).hasSize(0);

        assertThat(orderBook.getTopBidAsk().bid()).isNull();
        assertThat(orderBook.getTopBidAsk().bidSize()).isEqualTo(0);

        assertThat(orderBook.getTopBidAsk().ask()).isNull();
        assertThat(orderBook.getTopBidAsk().askSize()).isEqualTo(0);

        assertThat(tradeRecorder.getTrades()).hasSize(2)
                .usingFieldByFieldElementComparator()
                .extracting(
                        OrderBook.Trade::price,
                        OrderBook.Trade::size,
                        OrderBook.Trade::orderIds)
                .containsExactly(
                        tuple(100.0, 1, Set.of(buyOrder.id(), sellOrder.id())),
                        tuple(100.0, 1, Set.of(buyOrder2.id(), sellOrder2.id()))
                );
    }

    @ParameterizedTest
    @MethodSource("orderBooks")
    void shouldCreateTradeIfTwoOrdersCross(OrderBook orderBook) {
        orderBook.addTradeListener(tradeRecorder);

        //Given
        OrderBook.Order buyOrder = orderManager.createOrder(BUY, LIMIT, 101.0, 1);
        OrderBook.Order sellOrder = orderManager.createOrder(SELL, LIMIT, 99.0, 1);
        OrderBook.Order buyOrder2 = orderManager.createOrder(BUY, LIMIT, 101.0, 1);
        OrderBook.Order sellOrder2 = orderManager.createOrder(SELL, LIMIT, 99.0, 1);

        //when
        orderBook.addOrder(buyOrder);
        orderBook.addOrder(sellOrder);

        orderBook.addOrder(sellOrder2);
        orderBook.addOrder(buyOrder2);

        //Then
        assertThat(orderBook.getAllOrders()).hasSize(0);

        assertThat(orderBook.getTopBidAsk().bid()).isNull();
        assertThat(orderBook.getTopBidAsk().bidSize()).isEqualTo(0);

        assertThat(orderBook.getTopBidAsk().ask()).isNull();
        assertThat(orderBook.getTopBidAsk().askSize()).isEqualTo(0);

        assertThat(tradeRecorder.getTrades()).hasSize(2)
                .usingFieldByFieldElementComparator()
                .extracting(
                        OrderBook.Trade::price,
                        OrderBook.Trade::size,
                        OrderBook.Trade::orderIds)
                .containsExactly(
                        tuple(101.0, 1, Set.of(buyOrder.id(), sellOrder.id())),
                        tuple(99.0, 1, Set.of(buyOrder2.id(), sellOrder2.id()))
                );
    }


    @ParameterizedTest
    @MethodSource("orderBooks")
    void shouldMatchAllOrdersThatAreInOrderBook(OrderBook orderBook) {
        orderBook.addTradeListener(tradeRecorder);

        //Given
        OrderBook.Order buyOrder1 = orderManager.createOrder(BUY, LIMIT, 102.0, 1);
        OrderBook.Order buyOrder2 = orderManager.createOrder(BUY, LIMIT, 101.0, 2);
        OrderBook.Order buyOrder3 = orderManager.createOrder(BUY, LIMIT, 100.0, 4);
        OrderBook.Order sellOrder = orderManager.createOrder(SELL, LIMIT, 99.0, 7);

        //when
        orderBook.addOrder(buyOrder1);
        orderBook.addOrder(buyOrder2);
        orderBook.addOrder(buyOrder3);
        orderBook.addOrder(sellOrder);


        //Then
        assertThat(orderBook.getAllOrders()).hasSize(0);

        assertThat(orderBook.getTopBidAsk().bid()).isNull();
        assertThat(orderBook.getTopBidAsk().bidSize()).isEqualTo(0);

        assertThat(orderBook.getTopBidAsk().ask()).isNull();
        assertThat(orderBook.getTopBidAsk().askSize()).isEqualTo(0);

        assertThat(tradeRecorder.getTrades()).hasSize(3)
                .usingFieldByFieldElementComparator()
                .extracting(
                        OrderBook.Trade::price,
                        OrderBook.Trade::size,
                        OrderBook.Trade::orderIds)
                .containsExactly(
                        tuple(102.0, 1, Set.of(buyOrder1.id(), sellOrder.id())),
                        tuple(101.0, 2, Set.of(buyOrder2.id(), sellOrder.id())),
                        tuple(100.0, 4, Set.of(buyOrder3.id(), sellOrder.id()))
                );
    }


    @ParameterizedTest
    @MethodSource("orderBooks")
    void shouldMatchOrdersBookUpToLimitAndRestShouldStayInBook(OrderBook orderBook) {
        orderBook.addTradeListener(tradeRecorder);

        //Given
        OrderBook.Order sellOrder = orderManager.createOrder(SELL, LIMIT, 102.0, 4);
        OrderBook.Order buyOrder1 = orderManager.createOrder(BUY, LIMIT, 102.0, 1);
        OrderBook.Order buyOrder2 = orderManager.createOrder(BUY, LIMIT, 101.0, 2);

        //when
        orderBook.addOrder(buyOrder1);
        orderBook.addOrder(buyOrder2);
        orderBook.addOrder(sellOrder);


        //Then
        assertThat(orderBook.getAllOrders()).hasSize(2);

        assertThat(orderBook.getTopBidAsk().bid()).isEqualTo(101.0);
        assertThat(orderBook.getTopBidAsk().bidSize()).isEqualTo(2);

        assertThat(orderBook.getTopBidAsk().ask()).isEqualTo(102);
        assertThat(orderBook.getTopBidAsk().askSize()).isEqualTo(3);

        assertThat(tradeRecorder.getTrades()).hasSize(1)
                .usingFieldByFieldElementComparator()
                .extracting(
                        OrderBook.Trade::price,
                        OrderBook.Trade::size,
                        OrderBook.Trade::orderIds)
                .containsExactly(
                        tuple(102.0, 1, Set.of(buyOrder1.id(), sellOrder.id()))
                );
    }

    static class TradeRecorder implements OrderBook.OrderBookTradeListener {
        private final List<OrderBook.Trade> trades = new ArrayList<>();

        @Override
        public void onTrade(OrderBook.Trade trade) {
            trades.add(trade);
        }

        List<OrderBook.Trade> getTrades() {
            return trades;
        }
    }

}
