package com.mfruhrmann.orderbooks;

import com.mfruhrmann.orderbooks.api.OrderBook;
import com.mfruhrmann.orderbooks.impl.BasicOrderBook;
import com.mfruhrmann.orderbooks.utils.OrderManager;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class Main {

    private static final OrderManager ORDER_MANAGER = new OrderManager();

    public static void main(String[] args) {


        BasicOrderBook basicOrderBook = new BasicOrderBook();

        List<OrderBook.Trade> trades = new ArrayList<>();
        basicOrderBook.addTradeListener(trades::add);

        OrderManager orderManager = new OrderManager();

        IntStream.range(0, 1_000_000)
                .mapToObj(x -> orderManager.createOrder(OrderBook.Side.values()[(x + 1) % 2], OrderBook.OrderType.LIMIT, 100 + (x % 10) - 5, 1))
                .forEach(basicOrderBook::addOrder);

        System.out.println(basicOrderBook.getAllOrders());
        System.out.println(trades.size());

    }
}
