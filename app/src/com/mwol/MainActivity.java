package com.mwol;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HexFormat;

public class MainActivity extends AppCompatActivity
{
    private final String DefaultMac = "";
    private final String DefaultIp = "255.255.255.255";
    private final int DefaultWOLPort = 9;

    private final byte[] magicBytes =
      new byte[] { (byte)0xFF, (byte)0xFF, (byte)0xFF,
                   (byte)0xFF, (byte)0xFF, (byte)0xFF };

    private EditText etMac;
    private EditText etIp;
    private EditText etPort;

    private ItemAdapter adapter;

    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = findViewById(R.id.button);
        button.setOnClickListener(this::onSendButtonClick);

        ImageButton resetButton = findViewById(R.id.reset_button);
        resetButton.setOnClickListener(v -> {
            RotateAnimation rotate =
              new RotateAnimation(0f,
                                  360f,
                                  Animation.RELATIVE_TO_SELF,
                                  0.5f,
                                  Animation.RELATIVE_TO_SELF,
                                  0.5f);
            rotate.setDuration(500);
            v.startAnimation(rotate);

            this.resetETS();
        });

        this.etMac = findViewById(R.id.mac);
        this.etIp = findViewById(R.id.ip);
        this.etPort = findViewById(R.id.port);

        this.resetETS();

        RecyclerView recyclerView = findViewById(R.id.item_list);
        this.adapter =
          new ItemAdapter(this, this::onSendAgainButtonClick, recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void resetETS()
    {
        etMac.setText(this.DefaultMac);
        etIp.setText(this.DefaultIp);
        etPort.setText(String.format("%d", this.DefaultWOLPort));
    }

    private void onSendButtonClick(View v)
    {
        String mac = etMac.getText().toString();
        String ip = etIp.getText().toString();
        String portStr = etPort.getText().toString();

        int port = Integer.parseInt(etPort.getText().toString());

        new Thread(() -> {
            if (this.sendWOL(mac, ip, port)) {
                runOnUiThread(() -> {
                    this.adapter.addItem(new ListItem(mac, ip, portStr));
                    adapter.recyclerView.smoothScrollToPosition(0);
                });
            }
        }).start();
    }

    public void onSendAgainButtonClick(ListItem item)
    {
        new Thread(
          () -> this.sendWOL(item.mac, item.ip, Integer.parseInt(item.port)))
          .start();
    }

    private boolean sendWOL(String mac, String ip, int port)
    {
        byte[] macBytes;
        try {
            macBytes = HexFormat.of().parseHex(mac.replace(":", ""));
        } catch (IllegalArgumentException e) {
            new Handler(Looper.getMainLooper())
              .post(()
                      -> Toast
                           .makeText(this,
                                     "Invalid mac address: " + e.getMessage(),
                                     Toast.LENGTH_SHORT)
                           .show());
            return false;
        }

        byte[] buf = new byte[magicBytes.length + macBytes.length * 16];
        for (int i = 0; i < magicBytes.length; i++) {
            buf[i] = magicBytes[i];
        }

        int off = magicBytes.length;
        for (int j = 0; j < 16; j++) {
            for (int i = 0; i < macBytes.length; i++) {
                buf[i + off] = macBytes[i];
            }
            off += macBytes.length;
        }

        try {
            InetAddress address = InetAddress.getByName(ip);

            DatagramSocket socket = new DatagramSocket();

            DatagramPacket sendPacket =
              new DatagramPacket(buf, buf.length, address, port);

            socket.send(sendPacket);

            socket.close();
        } catch (IOException e) {
            Log.e("tag", e.toString());
            return false;
        }

        return true;
    }
}
