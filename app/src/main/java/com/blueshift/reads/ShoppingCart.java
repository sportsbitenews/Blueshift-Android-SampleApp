package com.blueshift.reads;

import android.content.Context;
import android.text.TextUtils;

import com.blueshift.reads.model.Book;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Rahul Raveendran V P
 *         Created on 6/1/17 @ 11:49 AM
 *         https://github.com/rahulrvp
 */

public class ShoppingCart {
    private static final String PREF_FILE = "ShoppingCartFile";
    private static final String PREF_KEY = "ShoppingCartKey";

    private static ShoppingCart ourInstance = null;
    private Map<String, Book> mProductsMap;

    private ShoppingCart() {
        mProductsMap = new HashMap<>();
    }

    public static ShoppingCart getInstance(Context context) {
        if (ourInstance == null) {
            ourInstance = load(context);
        }

        if (ourInstance == null) {
            ourInstance = new ShoppingCart();
        }

        return ourInstance;
    }

    private static ShoppingCart load(Context context) {
        ShoppingCart instance = null;

        if (context != null) {
            String json = context
                    .getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                    .getString(PREF_KEY, null);

            if (!TextUtils.isEmpty(json)) {
                try {
                    instance = new Gson().fromJson(json, ShoppingCart.class);
                } catch (Exception ignore) {

                }
            }
        }

        return instance;
    }

    public void save(Context context) {
        if (context != null) {
            context
                    .getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                    .edit()
                    .putString(PREF_KEY, new Gson().toJson(this))
                    .apply();
        }
    }

    public void add(Book book) {
        if (book != null) {
            mProductsMap.put(book.getSku(), book);
        }
    }

    public void remove(Book book) {
        if (book != null) {
            mProductsMap.remove(book.getSku());
        }
    }

    public void increaseQuantity(Book product) {
        if (product != null) {
            String sku = product.getSku();

            Book p1 = mProductsMap.get(sku);
            if (p1 != null) {
                p1.setQuantity(p1.getQuantity() + 1);

                mProductsMap.put(sku, p1);
            } else {
                add(product);
            }
        }
    }

    public void decreaseQuantity(Book product) {
        if (product != null) {
            String sku = product.getSku();
            int qty = product.getQuantity();

            if (qty > 0) {
                Book p1 = mProductsMap.get(sku);
                if (p1 != null) {
                    p1.setQuantity(p1.getQuantity() - 1);

                    mProductsMap.put(sku, p1);
                } else {
                    add(product);
                }
            } else {
                remove(product);
            }
        }
    }
}
