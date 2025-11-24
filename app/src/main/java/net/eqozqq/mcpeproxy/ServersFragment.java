package net.eqozqq.mcpeproxy;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuInflater;

import androidx.annotation.Nullable;

import java.util.List;

public class ServersFragment extends Fragment {

    private ListView listViewServers;
    private ServerAdapter adapter;
    private List<Server> serverList;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_servers, container, false);
        setHasOptionsMenu(true);

        listViewServers = view.findViewById(R.id.listViewServers);
        loadServers();

        listViewServers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Server selectedServer = serverList.get(position);
                startProxyForServer(selectedServer.getAddress(), selectedServer.getPort());
            }
        });

        registerForContextMenu(listViewServers);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem addItem = menu.add(Menu.NONE, 1, Menu.NONE, R.string.add_server);
        addItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            showAddEditServerDialog(null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.listViewServers) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle(serverList.get(info.position).toString());
            menu.add(Menu.NONE, 0, 0, R.string.edit_server);
            menu.add(Menu.NONE, 1, 1, R.string.delete_server);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int index = info.position;
        Server server = serverList.get(index);

        switch (item.getItemId()) {
            case 0:
                showAddEditServerDialog(server);
                return true;
            case 1:
                deleteServer(server);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void loadServers() {
        serverList = ServerStorage.loadServers(getActivity());
        adapter = new ServerAdapter(getActivity(), serverList);
        listViewServers.setAdapter(adapter);
    }

    private void showAddEditServerDialog(@Nullable final Server serverToEdit) {
        Context context = getActivity();
        if (context == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_edit_server, null);
        builder.setView(dialogView);

        final EditText editTextAddress = dialogView.findViewById(R.id.dialogEditTextAddress);
        final EditText editTextPort = dialogView.findViewById(R.id.dialogEditTextPort);

        if (serverToEdit != null) {
            builder.setTitle(R.string.dialog_title_edit);
            editTextAddress.setText(serverToEdit.getAddress());
            editTextPort.setText(String.valueOf(serverToEdit.getPort()));
        } else {
            builder.setTitle(R.string.dialog_title_add);
        }

        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String address = editTextAddress.getText().toString().trim();
                String portStr = editTextPort.getText().toString().trim();

                if (address.isEmpty()) {
                    Toast.makeText(context, R.string.error_invalid_address, Toast.LENGTH_SHORT).show();
                    return;
                }

                int port;
                try {
                    port = Integer.parseInt(portStr);
                    if (port < 1 || port > 65535) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(context, R.string.error_invalid_port, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (serverToEdit != null) {
                    serverToEdit.setAddress(address);
                    serverToEdit.setPort(port);
                } else {
                    Server newServer = new Server(address, port);
                    serverList.add(newServer);
                }

                ServerStorage.saveServers(context, serverList);
                adapter.notifyDataSetChanged();
            }
        });

        builder.setNegativeButton(R.string.cancel, null);
        builder.create().show();
    }

    private void deleteServer(Server server) {
        serverList.remove(server);
        ServerStorage.saveServers(getActivity(), serverList);
        adapter.notifyDataSetChanged();
        Toast.makeText(getActivity(), "Server deleted: " + server.toString(), Toast.LENGTH_SHORT).show();
    }

    private void startProxyForServer(String address, int port) {
        HomeFragment homeFragment = (HomeFragment) getFragmentManager().findFragmentByTag(MainActivity.TAB_TAG_HOME);
        if (homeFragment != null) {
            homeFragment.startProxy(address, port);
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).switchToTab(MainActivity.TAB_TAG_HOME);
            }
        } else {
            Context context = getActivity();
            if (context != null) {
                startProxyService(context, address, port);
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchToTab(MainActivity.TAB_TAG_HOME);
                }
            }
        }
    }

    private void startProxyService(Context context, String address, int port) {
        android.content.Intent serviceIntent = new android.content.Intent(context, ProxyService.class);
        serviceIntent.setAction(ProxyService.ACTION_START);
        serviceIntent.putExtra(ProxyService.EXTRA_ADDRESS, address);
        serviceIntent.putExtra(ProxyService.EXTRA_PORT, port);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}