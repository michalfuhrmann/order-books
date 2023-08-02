package com.mfruhrmann.orderbooks;

import com.mfruhrmann.orderbooks.api.OrderBook;
import com.mfruhrmann.orderbooks.impl.BasicOrderBook;
import com.mfruhrmann.orderbooks.utils.OrderManager;

import java.util.stream.IntStream;

public class Main {

    private static final OrderManager ORDER_MANAGER = new OrderManager();

    public static void main(String[] args) {


        BasicOrderBook basicOrderBook = new BasicOrderBook();

        OrderManager orderManager = new OrderManager();
        IntStream.range(0, 100)
                .mapToObj(x -> orderManager.createOrder(OrderBook.Side.values()[(x + 1) % 2], OrderBook.OrderType.LIMIT, 100 + (x % 10) - 5, 1))
                .forEach(o -> basicOrderBook.addOrder(o));
//        basicOrderBook.addOrder(ORDER_MANAGER.createOrder(BUY, LIMIT, 100.0, 1));
//        basicOrderBook.addOrder(ORDER_MANAGER.createOrder(SELL, LIMIT, 101.0, 1));

        System.out.println(basicOrderBook.getAllOrders());

    }
}
