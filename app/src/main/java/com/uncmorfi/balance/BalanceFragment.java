package com.uncmorfi.balance;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.uncmorfi.R;
import com.uncmorfi.balance.barcode.BarcodeReaderActivity;
import com.uncmorfi.balance.userModel.User;
import com.uncmorfi.balance.userModel.UsersDbHelper;

public class BalanceFragment extends Fragment implements UserCursorAdapter.OnCardClickListener,
        DownloadUserAsyncTask.DownloadUserListener {
    final static private int REQUEST_CODE = 1;

    private UsersDbHelper mUsersDbHelper;

    private RecyclerView mRecyclerView;
    private UserCursorAdapter mUserCursorAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_balance, container, false);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.user_list);

        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mUserCursorAdapter = new UserCursorAdapter(getContext(), this);
        mRecyclerView.setAdapter(mUserCursorAdapter);

        mUsersDbHelper = new UsersDbHelper(getContext());

        // TODO usar un theard o un loader para mejorar la performance
        mUserCursorAdapter.swapCursor(mUsersDbHelper.getAllUsers());

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.balance, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_new_user:
                new NewUserDialog().show(getFragmentManager(), "NewUserDialog");
                break;
            case R.id.action_new_user_camera:
                Intent i = new Intent(getActivity(), BarcodeReaderActivity.class);
                startActivityForResult(i, REQUEST_CODE);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(UserCursorAdapter.UserViewHolder holder) {
        final String card = holder.cardText.getText().toString();
        final String name = holder.nameText.getText().toString();
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        final CharSequence[] items = new CharSequence[2];

        items[0] = "Eliminar";
        items[1] = "Modificar nombre";

        builder.setTitle("Tarjeta")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            createDeleteDialog(card, name).show();
                        } else if (which == 1) {
                            createSetNameDialog(card, name);
                        }
                    }
                })
                .create()
                .show();
    }

    private AlertDialog createDeleteDialog(final String card, final String name) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Eliminar tarjeta de " + name + "?")
                .setPositiveButton("ELIMINAR",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                UsersDbHelper usersDbHelper = new UsersDbHelper(getContext());
                                usersDbHelper.deleteUserByCard(card);
                                mUserCursorAdapter.swapCursor(usersDbHelper.getAllUsers());
                            }
                        })
                .setNegativeButton("CANCELAR",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Nope
                            }
                        });
        return builder.create();
    }

    private void createSetNameDialog(String card, String name) {
        Bundle bundle = new Bundle();
        bundle.putString("card", card);
        bundle.putString("oldName", name);
        SetNameDialog setNameDialog = new SetNameDialog();
        setNameDialog.setArguments(bundle);
        setNameDialog.show(getFragmentManager(), "SetNameDialog");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                newUser(data.getStringExtra(BarcodeReaderActivity.REQUEST_DATA));
            }
        }
    }

    public void newUser(String card) {
        Log.i("BalanceFragment", "New card " + card);
        new DownloadUserAsyncTask(this).execute(card);
    }

    public void setName(String card, String name) {
        Cursor cursor = mUsersDbHelper.getUserByCard(card);
        if (cursor.moveToNext()) {
            // Generar el usurio
            User user = new User(cursor);

            // Cambiarle el nombre y guardarlo
            user.setName(name);
            mUsersDbHelper.updateUserName(user);

            // Volver a consultar la base de datos
            mUserCursorAdapter.swapCursor(mUsersDbHelper.getAllUsers());
        } else {
            Toast.makeText(getContext(),
                    getContext().getString(R.string.refresh_fail), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onUserDownloaded(User user) {
        if (user != null) {
            // Guardar en la base de datos
            mUsersDbHelper.saveNewUser(user);

            // Volver a consultar la base de datos
            mUserCursorAdapter.swapCursor(mUsersDbHelper.getAllUsers());

            Toast.makeText(getContext(),
                    getContext().getString(R.string.new_user_success), Toast.LENGTH_SHORT)
                    .show();
        } else {
            Toast.makeText(getContext(),
                    getContext().getString(R.string.new_user_fail), Toast.LENGTH_LONG)
                    .show();
        }
    }

}