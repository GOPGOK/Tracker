package com.m.mobilelocation;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

// класс реализует интерфейс LocationListener что позволяет получать и отслеживать изменения в местоположении устройства
public class MainActivity extends AppCompatActivity implements LocationListener {

    // объект класса locationManager для получения и отслеживания местоположения устройства
    private LocationManager locationManager;
    // переменная для хранения широты
    private double latitude;
    // переменная для хранения долготы
    private double longitude;
    // кнопка отправки локации
    private Button btnSendLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // связываем кнопку с кнопкой в xml
        btnSendLocation = findViewById(R.id.btn_send_location);
        // создаем объект locationManager для получения и отслеживания местоположения устройства с
        // использованием системного сервиса LOCATION_SERVICE
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Проверяем наличие разрешений у приложения на получение местоположения устройства
        // (для новых версий API мы должны одновременно проверять
        // ACCESS_FINE_LOCATION (точное местоположение) и ACCESS_COARSE_LOCATION (примерное местоположение)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // в случае если у приложения нет нужных разршений создаем запрос
            // locationPermissionRequest на получение нужных разрешений у пользователя
            ActivityResultLauncher<String[]> locationPermissionRequest =
                    registerForActivityResult(new ActivityResultContracts
                                    .RequestMultiplePermissions(), result -> {
                                Boolean fineLocationGranted = null;
                                // запрос необходим для устройств с версией API больше N (Api 24, Android 7),
                                // прописываем услоивие для устройств подходящих под критерий
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                    fineLocationGranted = result.getOrDefault(
                                            Manifest.permission.ACCESS_FINE_LOCATION, false);
                                }
                                Boolean coarseLocationGranted = null;
                                // запрос необходим для устройств с версией API больше N (Api 24, Android 7),
                                // прописываем услоивие для устройств подходящих под критерий
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                    coarseLocationGranted = result.getOrDefault(
                                            Manifest.permission.ACCESS_COARSE_LOCATION, false);
                                }
                                if (fineLocationGranted != null && fineLocationGranted) {
                                    // В случае получения от пользователя разрешения на точное местоположение
                                    // получаем расположение при помощи locationManager используя GPS или местоположение в сети
                                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
                                    // задаем обработчик для кнопки отправки локации
                                    setButtonClickListener();
                                } else if (coarseLocationGranted != null && coarseLocationGranted) {
                                    // В случае получения от пользователя разрешения на примерное местположение
                                    Toast.makeText(this, "Приложение нельзя использовать с приблизительным местоположением!", Toast.LENGTH_SHORT).show();
                                } else {
                                    // В случае если не получено разрешение на получение местоположения
                                    Toast.makeText(this, "Приложение нельзя использовать без доступа к данным о местоположении!", Toast.LENGTH_SHORT).show();
                                }
                            }
                    );

            // вызов запроса разрешений на местоположение от пользователя
            locationPermissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } else {
            // В случае если уже получено от пользователя разрешения на точное местоположение
            // получаем расположение при помощи locationManager используя GPS или местоположение в сети
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            // задаем обработчик для кнопки отправки локации
            setButtonClickListener();
        }
    }

    // Обработчик нажатия на кнопку отправки данных о местоположении
    private void setButtonClickListener() {
        btnSendLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // в случае если locationManager еще не успел обновить данные по умолчанию для местоположения
                // выводим пользователю сообщение о том что данные в процеессе обновления
                if (latitude == 0 && longitude == 0) {
                    Toast.makeText(MainActivity.this, "Обновленние данных о местоположении, попробуйте позже!", Toast.LENGTH_SHORT).show();
                }
                // иначе вызываем отправку данных на сервер
                else if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    sendLocation();
                }
                // в случае если не выданы разрешения приложению на получение данных о местоположении выводим предупреждение
                else {
                    Toast.makeText(MainActivity.this, "Необходимо выдать приложению разрешения для получения точного местоположения!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // функция отправки данных о местоложении
    private void sendLocation() {
        // получаем url маршрут к api серверного приложения из файла ресурсов
        String url = getResources().getString(R.string.api_url);
        // Формируем HashMap с отправляемыми параметрами (широта, долгота)
        Map<String, Double> paramsMap = new HashMap();
        paramsMap.put("latitude", latitude);
        paramsMap.put("longitude", longitude);

        // Создаем на основе объекта HashMap Json объект для отправки
        JSONObject parameters = new JSONObject(paramsMap);

        // Создаем объект-Json Запрос, в качестве параметров исползуем метод запроса POST,
        // url для доступа к Api серверного приложения, JSON объект с отправляемыми параметрами,
        // новый объект слушатель для получения ответа от серверного приложения
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, parameters, new Response.Listener<JSONObject>() {
            // в случае получения ответа от сервера о успешном получении данных выводим
            // пользователю информацию которая была отправлена и то что она была успешно отправлена
            @Override
            public void onResponse(JSONObject response) {
                Toast.makeText(MainActivity.this, "Широта: " + latitude + "\nДолгота:" + longitude, Toast.LENGTH_LONG).show();
                Toast.makeText(MainActivity.this, "Местоположение успешно отправлено!", Toast.LENGTH_LONG).show();
            }
        }, new Response.ErrorListener() {
            // в случае если сервер вернул ошибку или не был получен ответ
            // уведомляем пользователя о том что не удалось отправить местоположение
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this,
                        "Не удалось отправить местоположение!",
                        Toast.LENGTH_SHORT).show();
            }
        });
        VolleySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest);
    }

    // перегруженный метод вызываемый при изменении местоположения устройства
    @Override
    public void onLocationChanged(@NonNull Location location) {
        // когда меняется местоположение устройства переопределяем координаты широты, долготы
        latitude = location.getLatitude();
        longitude = location.getLongitude();
    }

    // перегруженные методы для отслеживания провайдера позволяющего получить местоположение
    // в случае если провайдер доступен в логе будет
    // "Location", "Enable"
    // иначе
    // "Location", "Disable"

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        LocationListener.super.onProviderEnabled(provider);
        Log.d("Location", "Disable");
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        LocationListener.super.onProviderDisabled(provider);
        Log.d("Location", "Enable");
    }
}