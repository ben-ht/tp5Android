package com.example.tpimagehembert;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.FileNotFoundException;

public class MainActivity extends AppCompatActivity {

    //region CONST
    /// Valeur maximale d'un pixel
    private final int MAX_PIXEL_VALUE = 255;
    //endregion CONST

    //region PROPERTIES
    /// Elément image de l'écran
    private ImageView image;

    /// Image chargée
    private Bitmap loadedImage;

    /// Types de transformation miroir possibles sur une image
    private enum Mirror{
        HORIZONTAL,
        VERTICAL
    }

    /// Directions de rotation de l'image
    private enum Rotation {
        LEFT,
        RIGHT
    }

    /**
     * Récupère l'image d'une autre activité
     */
    private final ActivityResultLauncher<String> getImageUri = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri result) {
                    if (result != null){
                        try {
                            callback(result);
                        } catch (FileNotFoundException e) {
                            Toast.makeText(MainActivity.this,
                                    "Erreur lors de l'import de l'image",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
    );
    //endregion PROPERTIES


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        image = findViewById(R.id.image);
        registerForContextMenu(image);

        ImageButton uploadImageBtn = findViewById(R.id.uploadImage);
        uploadImageBtn.setOnClickListener(uploadImage());

        ImageButton cancelBtn = findViewById(R.id.cancel);
        cancelBtn.setOnClickListener(cancel());
    }

    //region PUBLIC OVERRIDE METHODS
    /**
     * Crée le menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    /**
     * Gère les clics sur les boutons du menu
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item){
        if (image.getDrawable() == null){
            return true;
        }
        int id = item.getItemId();
        if (id == R.id.horizontal_mirror){
            Bitmap bitmap = ((BitmapDrawable)image.getDrawable()).getBitmap();
            Bitmap newImage = mirror(bitmap, Mirror.HORIZONTAL);
            image.setImageBitmap(newImage);
            return true;
        } else if (id == R.id.vertical_mirror){
            Bitmap bitmap = ((BitmapDrawable)image.getDrawable()).getBitmap();
            Bitmap newImage = mirror(bitmap, Mirror.VERTICAL);
            image.setImageBitmap(newImage);
            return true;
        }  else if (item.getItemId() == R.id.rotate_right){
            Bitmap bitmap = ((BitmapDrawable) image.getDrawable()).getBitmap();
            Bitmap newImage = rotate(bitmap, Rotation.RIGHT);
            image.setImageBitmap(newImage);
            return true;
        } else if (item.getItemId() == R.id.rotate_left){
            Bitmap bitmap = ((BitmapDrawable) image.getDrawable()).getBitmap();
            Bitmap newImage = rotate(bitmap, Rotation.LEFT);
            image.setImageBitmap(newImage);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Crée le menu contextuel de l'image
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu,
                                    View v,
                                    ContextMenu.ContextMenuInfo menuInfo){
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.image_menu, menu);
    }

    /**
     * Gère les clics sur les boutons du menu contextuel de l'image
     */
    @Override
    public boolean onContextItemSelected(MenuItem item){
        if (item.getItemId() == R.id.invert){
            Bitmap bitmap = ((BitmapDrawable)image.getDrawable()).getBitmap();
            Bitmap newImage = invert(bitmap);
            image.setImageBitmap(newImage);
            return true;
        } else if (item.getItemId() == R.id.greyscale) {
            Bitmap bitmap = ((BitmapDrawable) image.getDrawable()).getBitmap();
            Bitmap newImage = greyscale(bitmap);
            image.setImageBitmap(newImage);
            return true;
        }

        return super.onContextItemSelected(item);
    }
    //endregion PUBLIC OVERRIDE METHODS

    //region CLICK EVENTS
    /**
     * Evénement déclenché au clic sur le bouton d'ajout d'image
     */
    private View.OnClickListener uploadImage() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getImageUri.launch("image/*");
            }
        };
    }

    /**
     * Restaure l'image à l'état initial
     */
    private View.OnClickListener cancel(){
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (image.getDrawable() != null) {
                    image.setImageBitmap(loadedImage);
                }
            }
        };
    }
    //endregion CLICK EVENTS

    //region IMAGE TRANSFORMATION METHODS
    /**
     * Applique un effet miroir sur un Bitmap
     * @param original Bitmap original
     * @param mirror Effet miroir vertical ou horizontal
     * @return Image transformée
     */
    private Bitmap mirror(Bitmap original, Mirror mirror){
        int width = original.getWidth();
        int height = original.getHeight();

        Bitmap newImage = Bitmap.createBitmap(width,
                height,
                original.getConfig());

        for (int i = 0; i < width; i++){
            for (int j = 0; j < height; j++){
                int pixelColor = original.getPixel(i, j);
                if (mirror == Mirror.HORIZONTAL) {
                    newImage.setPixel(width - i - 1, j, pixelColor);
                } else if (mirror == Mirror.VERTICAL){
                    newImage.setPixel(i, height - j - 1, pixelColor);
                }
            }
        }

        return newImage;
    }

    /**
     * Inverse les couleurs de l'image
     * @param original Image originale
     * @return Image transformée
     */
    private Bitmap invert(Bitmap original) {
        int width = original.getWidth();
        int height = original.getHeight();

        Bitmap newImage = Bitmap.createBitmap(width,
                height,
                original.getConfig());

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int originalColor = original.getPixel(i, j);
                int r = MAX_PIXEL_VALUE - Color.red(originalColor);
                int g = MAX_PIXEL_VALUE - Color.green(originalColor);
                int b = MAX_PIXEL_VALUE - Color.blue(originalColor);
                int color = Color.rgb(r, g, b);
                newImage.setPixel(i, j, color);
            }
        }

        return newImage;
    }

    /**
     * Applique un niveau de gris sur l'image
     * @param original Image originale
     * @return Image transformée
     */
    private Bitmap greyscale(Bitmap original) {
        int width = original.getWidth();
        int height = original.getHeight();

        Bitmap newImage = Bitmap.createBitmap(width,
                height,
                original.getConfig());

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int originalColor = original.getPixel(i, j);
                int r = Color.red(originalColor);
                int g = Color.green(originalColor);
                int b = Color.blue(originalColor);
                int average = (r + g + b) / 3;

                int color = Color.rgb(average, average, average);
                newImage.setPixel(i, j, color);
            }
        }

        return newImage;
    }

    /**
     * Applique une rotation de 90° sur l'image
     * @param original Image originale
     * @param rotation Droite ou gauche
     * @return Image transformée
     */
    private Bitmap rotate(Bitmap original, Rotation rotation){
        int width = original.getWidth();
        int height = original.getHeight();

        Bitmap newImage = Bitmap.createBitmap(height,
                width,
                original.getConfig());

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int color = original.getPixel(i, j);
                if (rotation == Rotation.LEFT){
                    newImage.setPixel(j, width - 1 - i, color);
                } else if (rotation == Rotation.RIGHT){
                    newImage.setPixel(height - 1 - j, i, color);
                }
            }
        }

        return newImage;
    }
    //endregion IMAGE TRANSFORMATION METHODS

    //region PRIVATE METHODS
    /**
     * Affiche une image à l'écran
     * @param imageUri Source de l'image
     * @throws FileNotFoundException Si l'emplacement n'existe pas
     */
    private void chargerImage(Uri imageUri) throws FileNotFoundException {
        BitmapFactory.Options option = new BitmapFactory.Options();
        option.inMutable = true;
        Bitmap bm = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri),
                null,
                option);

        loadedImage = bm;
        image.setImageBitmap(bm);
    }

    /**
     * Fonction callback de l'upload d'image
     * @param imageUri Source de l'image
     * @throws FileNotFoundException Si l'emplacement n'existe pas
     */
    public void callback(Uri imageUri) throws FileNotFoundException {
        TextView uriTextView = findViewById(R.id.uri);
        uriTextView.setText(imageUri.toString());

        chargerImage(imageUri);
    }
    //endregion PRIVATE METHODS


}