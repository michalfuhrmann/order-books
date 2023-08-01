package com.mfruhrmann.orderbooks;

import com.mfruhrmann.orderbooks.impl.BasicOrderBook;
import com.mfruhrmann.orderbooks.utils.OrderManager;

import static com.mfruhrmann.orderbooks.api.OrderBook.OrderType.LIMIT;
import static com.mfruhrmann.orderbooks.api.OrderBook.Side.BUY;
import static com.mfruhrmann.orderbooks.api.OrderBook.Side.SELL;

public class Main {

    private static final OrderManager ORDER_MANAGER = new OrderManager();

    public static void main(String[] args) {


        BasicOrderBook basicOrderBook = new BasicOrderBook();
        basicOrderBook.addOrder(ORDER_MANAGER.createOrder(BUY, LIMIT, 100.0, 1));
        basicOrderBook.addOrder(ORDER_MANAGER.createOrder(SELL, LIMIT, 101.0, 1));

        System.out.println(basicOrderBook.getAllOrders());

    }
}
