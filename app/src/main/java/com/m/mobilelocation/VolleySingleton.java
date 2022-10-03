package com.m.mobilelocation;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

// класс для создания очереди запросов привязанной к контексту к серверу с использованием библиотеки volley
public class VolleySingleton {
    // экземпляр класса-очереди запросов
    private static VolleySingleton instance;
    // очередь запросов
    private RequestQueue requestQueue;
    // контекст для которого используется очередь
    private static Context context;

    // конструктор класса очереди запросов привязанной к контексту, в качестве параметра принимает контекст
    private VolleySingleton(Context context) {
        VolleySingleton.context = context;
        requestQueue = getRequestQueue();
    }

    // метод получения экзмепляра класса-очереди запросов в качестве параметра принимает объект
    // класса Context
    public static synchronized VolleySingleton getInstance(Context context) {
        if (instance == null) {
            instance = new VolleySingleton(context);
        }
        return instance;
    }

    // метод для создания очереди запросов, возвращает очередь запросов
    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }
        return requestQueue;
    }

    // метод для добавления запроса произвольного типа Т в очередь
    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
}
