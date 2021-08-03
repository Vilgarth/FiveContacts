package com.example.fivecontacts.main.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.example.fivecontacts.R;
import com.example.fivecontacts.main.model.Contato;
import com.example.fivecontacts.main.model.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AlterarContatos_Activity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    Boolean primeiraVezUser=true;
    EditText edtNome;
    ListView lv;
    BottomNavigationView bnv;
    User user;
    ImageButton removeBut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alterar_contatos);
        edtNome = findViewById(R.id.edtBusca);
        bnv = findViewById(R.id.bnv);
        bnv.setOnNavigationItemSelectedListener(this);
        bnv.setSelectedItemId(R.id.anvMudar);
        removeBut = findViewById(R.id.removeButton);

        //Dados da Intent Anterior
        Intent quemChamou=this.getIntent();
        if (quemChamou!=null) {
            Bundle params = quemChamou.getExtras();
            if (params!=null) {
                //Recuperando o Usuario
                user = (User) params.getSerializable("usuario");
                setTitle("Alterar Contatos de Emergência");
            }
        }
        lv = findViewById(R.id.listContatosDoCell);
        //Evento de limpar Componente
        edtNome.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (primeiraVezUser){
                    primeiraVezUser=false;
                    edtNome.setText("");
                }

                return false;
            }
        });

        removeBut.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                preencherListViewImagens(user);
                return false;
            }
        });
    }

    public void salvarContato (Contato w){
        SharedPreferences salvaContatos =
                getSharedPreferences("contatos",Activity.MODE_PRIVATE);

        int num = salvaContatos.getInt("numContatos", 0); //checando quantos contatos já tem
        SharedPreferences.Editor editor = salvaContatos.edit();
        try {
            ByteArrayOutputStream dt = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(dt);
            dt = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(dt);
            oos.writeObject(w);
            String contatoSerializado= dt.toString(StandardCharsets.ISO_8859_1.name());
            editor.putString("contato"+(num+1), contatoSerializado);
            editor.putInt("numContatos",num+1);
        }catch(Exception e){
            e.printStackTrace();
        }
        editor.commit();
        user.getContatos().add(w);
    }


    public void removeContato (Contato w){
        SharedPreferences removeContatos =
                getSharedPreferences("contatos",Activity.MODE_PRIVATE);

        int num = removeContatos.getInt("numContatos", 0); //checando quantos contatos já tem
        SharedPreferences.Editor editor = removeContatos.edit();
        try {
            ByteArrayOutputStream dt = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(dt);
            dt = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(dt);
            oos.writeObject(w);
            String contatoSerializado= dt.toString(StandardCharsets.ISO_8859_1.name());
            editor.remove(contatoSerializado);
            editor.putInt("numContatos",num-1);
        }catch(Exception e){
            e.printStackTrace();
        }
        editor.commit();
        user.getContatos().remove(w);
    }



    @Override
    protected void onStop() {
        super.onStop();
        Log.v("PDM","Matando a Activity Lista de Contatos");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v("PDM","Matei a Activity Lista de Contatos");
    }


    public void onClickBuscar(View v){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED) {
            Log.v("PDM", "Pedir permissão");
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, 3333);
            return;
        }
        Log.v("PDM", "Tenho permissão");

        ContentResolver cr = getContentResolver();
        String consulta = ContactsContract.Contacts.DISPLAY_NAME + " LIKE ?";
        String [] argumentosConsulta= {"%"+edtNome.getText()+"%"};
        Cursor cursor= cr.query(ContactsContract.Contacts.CONTENT_URI, null,
                consulta,argumentosConsulta, null);
        final String[] nomesContatos = new String[cursor.getCount()];
        final String[] telefonesContatos = new String[cursor.getCount()];
        Log.v("PDM","Tamanho do cursor:"+cursor.getCount());

        int i=0;
        while (cursor.moveToNext()) {
            int indiceNome = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME);
            String contatoNome = cursor.getString(indiceNome);
            Log.v("PDM", "Contato " + i + ", Nome:" + contatoNome);
            nomesContatos[i]= contatoNome;
            int indiceContatoID = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID);
            String contactID = cursor.getString(indiceContatoID);
            String consultaPhone = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactID;
            Cursor phones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null, consultaPhone, null, null);

            while (phones.moveToNext()) {
                String number = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                telefonesContatos[i]=number; //Salvando só último telefone
            }
            i++;
        }

        if (nomesContatos !=null) {
            for(int j=0; j<=nomesContatos.length; j++) {
                ArrayAdapter<String> adaptador;
                adaptador = new ArrayAdapter<String>(this, R.layout.list_view_layout, nomesContatos);
                lv.setAdapter(adaptador);
                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        Contato c= new Contato();
                        c.setNome(nomesContatos[i]);
                        c.setNumero("tel:+"+telefonesContatos[i]);
                        salvarContato(c);
                        Intent intent = new Intent(getApplicationContext(), ListaDeContatos_Activity.class);
                        intent.putExtra("usuario", user);
                        startActivity(intent);
                        finish();

                    }
                });
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Checagem de o Item selecionado é o do perfil
        if (item.getItemId() == R.id.anvPerfil) {
            //Abertura da Tela MudarDadosUsario
            Intent intent = new Intent(this, PerfilUsuario_Activity.class);
            intent.putExtra("usuario", user);
            startActivity(intent);

        }
        // Checagem de o Item selecionado é o do perfil
        if (item.getItemId() == R.id.anvLigar) {
            //Abertura da Tela Mudar COntatos
            Intent intent = new Intent(this, ListaDeContatos_Activity.class);
            intent.putExtra("usuario", user);
            startActivity(intent);

        }
        return true;
    }

    protected void atualizarListaDeContatos(User user){
        SharedPreferences recuperarContatos = getSharedPreferences("contatos", Activity.MODE_PRIVATE);

        int num = recuperarContatos.getInt("numContatos", 0);
        ArrayList<Contato> contatos = new ArrayList<Contato>();

        Contato contato;


        for (int i = 1; i <= num; i++) {
            String objSel = recuperarContatos.getString("contato" + i, "");
            if (objSel.compareTo("") != 0) {
                try {
                    ByteArrayInputStream bis =
                            new ByteArrayInputStream(objSel.getBytes(StandardCharsets.ISO_8859_1.name()));
                    ObjectInputStream oos = new ObjectInputStream(bis);
                    contato = (Contato) oos.readObject();

                    if (contato != null) {
                        contatos.add(contato);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }


        }
        Log.v("PDM3","contatos:"+contatos.size());
        user.setContatos(contatos);
    }
    protected  void preencherListViewImagens(final User user){

        final ArrayList<Contato> contatos = user.getContatos();
        Collections.sort(contatos);
        if (contatos != null) {
            String[] contatosNomes, contatosAbrevs;
            contatosNomes = new String[contatos.size()];
            contatosAbrevs= new String[contatos.size()];
            Contato c;
            for (int j = 0; j < contatos.size(); j++) {
                contatosAbrevs[j] =contatos.get(j).getNome().substring(0, 1);
                contatosNomes[j] =contatos.get(j).getNome();
            }
            ArrayList<Map<String,Object>> itemDataList = new ArrayList<Map<String,Object>>();;

            for(int i =0; i < contatos.size(); i++) {
                Map<String,Object> listItemMap = new HashMap<String,Object>();
                listItemMap.put("imageId", R.drawable.ic_action_ligar_list);
                listItemMap.put("contato", contatosNomes[i]);
                listItemMap.put("abrevs",contatosAbrevs[i]);
                itemDataList.add(listItemMap);
            }
            SimpleAdapter simpleAdapter = new SimpleAdapter(this,itemDataList,R.layout.list_view_layout_imagem,
                    new String[]{"imageId","contato","abrevs"},new int[]{R.id.userImage, R.id.userTitle,R.id.userAbrev});

            lv.setAdapter(simpleAdapter);


            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                    removeContato(contatos.get(i));
                    preencherListViewImagens(user);

                }
            });

        }


    }
    protected void preencherListView(final User user) {

        final ArrayList<Contato> contatos = user.getContatos();

        if (contatos != null) {
            final String[] nomesSP;
            nomesSP = new String[contatos.size()];
            Contato c;
            for (int j = 0; j < contatos.size(); j++) {
                nomesSP[j] = contatos.get(j).getNome();
            }

            ArrayAdapter<String> adaptador;

            adaptador = new ArrayAdapter<String>(this, R.layout.list_view_layout, nomesSP);

            lv.setAdapter(adaptador);


            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                    removeContato(contatos.get(i));
                    preencherListViewImagens(user);
                }
            });
        }//fim do IF do tamanho de contatos
    }

}