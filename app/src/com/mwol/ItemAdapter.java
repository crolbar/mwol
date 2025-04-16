package com.mwol;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder>
{
    private final List<ListItem> items;
    private final Context context;
    private final Consumer<ListItem> onClick;
    public final RecyclerView recyclerView;

    private static final String PREFS_NAME = "mwol_prefs";
    private static final String LIST_KEY = "mwol_hist_list";

    public ItemAdapter(Context ctx,
                       Consumer<ListItem> c,
                       RecyclerView recyclerView)
    {
        this.context = ctx;
        this.items = getStoredList(ctx);
        this.onClick = c;
        this.recyclerView = recyclerView;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView mac, ip, port;
        Button btn;

        public ViewHolder(View itemView)
        {
            super(itemView);
            mac = itemView.findViewById(R.id.item_mac);
            ip = itemView.findViewById(R.id.item_ip);
            port = itemView.findViewById(R.id.item_port);
            btn = itemView.findViewById(R.id.item_button);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                         int viewType)
    {
        View v = LayoutInflater.from(context).inflate(
          R.layout.list_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        ListItem item = items.get(position);
        holder.mac.setText(item.mac);
        holder.ip.setText(item.ip);
        holder.port.setText(item.port);

        holder.btn.setOnClickListener(v -> {
            this.onClick.accept(item);
            int pos = holder.getAdapterPosition();

            items.remove(pos);
            notifyItemRemoved(pos);

            items.add(0, item);
            notifyItemInserted(0);

            recyclerView.smoothScrollToPosition(0);

            storeList();
        });
    }

    private static String serList(List<ListItem> list)
    {
        // mac,ip,port|mac,ip,port

        StringBuilder sb = new StringBuilder();
        for (ListItem item : list) {
            sb.append(item.mac)
              .append(",")
              .append(item.ip)
              .append(",")
              .append(item.port)
              .append("|");
        }

        return sb.toString();
    }

    private static List<ListItem> deList(String s)
    {
        ArrayList<ListItem> l = new ArrayList<ListItem>();

        String[] items = s.split("\\|");
        for (String item : items) {
            String[] fields = item.split(",");
            l.add(new ListItem(fields[0], fields[1], fields[2]));
        }

        return l;
    }
    private void storeList()
    {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(LIST_KEY, serList(this.items));
        editor.apply();
    }

    public static List<ListItem> getStoredList(Context context)
    {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String ser = prefs.getString(LIST_KEY, null);
        if (ser != null && !ser.isEmpty()) {
            return deList(ser);
        }
        return new ArrayList<ListItem>();
    }

    @Override public int getItemCount() { return items.size(); }

    public void addItem(ListItem item)
    {
        items.add(0, item);
        storeList();
        notifyItemInserted(0);
    }
}

class ListItem
{
    public String mac;
    public String ip;
    public String port;

    public ListItem(String m, String i, String p)
    {
        mac = m;
        ip = i;
        port = p;
    }
}
