package com.mfruhrmann.orderbooks.impl;


import com.mfruhrmann.orderbooks.api.OrderBook;
import com.mfruhrmann.orderbooks.utils.OrderManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junitpioneer.jupiter.cartesian.CartesianTest;

import java.util.stream.Stream;

import static com.mfruhrmann.orderbooks.api.OrderBook.OrderType.LIMIT;
import static com.mfruhrmann.orderbooks.api.OrderBook.Side.BUY;
import static com.mfruhrmann.orderbooks.api.OrderBook.Side.SELL;
import static org.assertj.core.api.Assertions.assertThat;

public class OrderBookTestAddOrderTest {

    private final OrderManager orderManager = new OrderManager();

    public static Stream<Arguments> orderBooks() {
        return Stream.of(Arguments.of(new BasicOrderBook()));
    }

    @ParameterizedTest
    @MethodSource("orderBooks")
    void test(OrderBook orderBook) {

        //Given
        OrderBook.Order order1 = orderManager.createOrder(BUY, LIMIT, 100.0, 1);
        OrderBook.Order order2 = orderManager.createOrder(SELL, LIMIT, 101.0, 1);

        //when
        orderBook.addOrder(order1);
        orderBook.addOrder(order2);

        //Then

        assertThat(orderBook.getAllOrders()).hasSize(2);
        assertThat(orderBook.getTopBidAsk().bid()).isEqualTo(100.0);
        assertThat(orderBook.getTopBidAsk().ask()).isEqualTo(101.0);

    }

}
