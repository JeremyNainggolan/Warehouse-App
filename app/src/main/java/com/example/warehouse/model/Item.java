package com.example.warehouse.model;

import java.util.ArrayList;
import java.util.Iterator;

public class Item {
    private static ArrayList<String> items = new ArrayList<>();

    public static void setItems(ArrayList<String> _items) {
        items = _items;
    }

    public static void setItem(String _item) {
        items.add(_item);
    }

    public static void destroy(boolean _isDestroy) {
        if (_isDestroy) {
            items.clear();
        }
    }

    public static void delete(String _batch) {
        // Use an iterator to safely remove items while iterating
        Iterator<String> iterator = items.iterator();
        while (iterator.hasNext()) {
            String item = iterator.next();
            if (item.equals(_batch)) {
                iterator.remove(); // Safe removal using the iterator
            }
        }
    }


    public static ArrayList<String> getItems() {
        return items;
    }
}
