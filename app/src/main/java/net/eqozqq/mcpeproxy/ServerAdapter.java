package net.eqozqq.mcpeproxy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class ServerAdapter extends ArrayAdapter<Server> {

    public ServerAdapter(Context context, List<Server> servers) {
        super(context, 0, servers);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Server server = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_server, parent, false);
        }

        TextView textServerInfo = convertView.findViewById(R.id.textServerInfo);
        textServerInfo.setText(server.toString());

        return convertView;
    }
}