package com.mfruhrmann.orderbooks;

import com.mfruhrmann.orderbooks.api.OrderBook;
import com.mfruhrmann.orderbooks.impl.BasicOrderBook;
import com.mfruhrmann.orderbooks.utils.OrderManager;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class Main {

    public static final int ORDERS = 10_000_000;
    private static final OrderManager ORDER_MANAGER = new OrderManager();

    public static void main(String[] args) throws InterruptedException {


//        Thread.sleep(10_000);
        OrderManager orderManager = new OrderManager();

//        OrderBook orderBook = new ListBasedOrderBook(1);
        OrderBook orderBook = new BasicOrderBook();

        AtomicInteger atomicInteger = new AtomicInteger();
        orderBook.addTradeListener(trade -> atomicInteger.incrementAndGet());


        IntStream.range(0, ORDERS)
                .mapToObj(x -> orderManager.createOrder(OrderBook.Side.values()[(x + 1) % 2], OrderBook.OrderType.LIMIT, 100 + (x % 10) - 5, 1))
                .forEach(orderBook::addOrder);

        System.out.println(orderBook.getAllOrders());
        System.out.println(atomicInteger.get());


//        basicOrderBook = new BasicOrderBook();
//        basicOrderBook.addTradeListener(trades::add);
//
//
//        IntStream.range(0, 1_000_000)
//                .mapToObj(x -> {
//                    OrderBook.Side value = OrderBook.Side.values()[x % 2];
//                    int delta = value == OrderBook.Side.BUY ? -(x % 5) : x % 5;
//                    return orderManager.createOrder(value, OrderBook.OrderType.LIMIT, 100 + delta, 1);
//                })
//                .forEach(basicOrderBook::addOrder);
//
//        System.out.println(basicOrderBook.getAllOrders().size());
//        System.out.println(trades.size());

    }
}
