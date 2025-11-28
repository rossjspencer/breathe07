package com.example.smartair;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.IOException;

public class PdfPreviewActivity extends AppCompatActivity {

    public static final String EXTRA_PATH = "PDF_PATH";

    private PdfRenderer renderer;
    private PdfRenderer.Page currentPage;
    private ParcelFileDescriptor fileDescriptor;
    private ImageView imageView;
    private TextView tvMeta;
    private Button btnPrev, btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_preview);

        imageView = findViewById(R.id.ivPdfPage);
        tvMeta = findViewById(R.id.tvPdfMeta);
        btnPrev = findViewById(R.id.btnPdfPrev);
        btnNext = findViewById(R.id.btnPdfNext);

        String path = getIntent().getStringExtra(EXTRA_PATH);
        if (path == null) {
            tvMeta.setText("No PDF path provided.");
            return;
        }

        File file = new File(path);
        if (!file.exists()) {
            tvMeta.setText("PDF not found at " + path);
            return;
        }

        try {
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(fileDescriptor);
            showPage(0);
        } catch (IOException e) {
            tvMeta.setText("Error opening PDF: " + e.getMessage());
        }

        btnPrev.setOnClickListener(v -> showPage(getCurrentIndex() - 1));
        btnNext.setOnClickListener(v -> showPage(getCurrentIndex() + 1));
    }

    private void showPage(int index) {
        if (renderer == null) return;
        if (index < 0 || index >= renderer.getPageCount()) return;

        // Close current page before opening another
        if (currentPage != null) currentPage.close();

        currentPage = renderer.openPage(index);
        Bitmap bmp = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);
        currentPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        imageView.setImageBitmap(bmp);

        String meta = "Page " + (index + 1) + " of " + renderer.getPageCount();
        tvMeta.setText(meta);
        btnPrev.setEnabled(index > 0);
        btnNext.setEnabled(index < renderer.getPageCount() - 1);
    }

    private int getCurrentIndex() {
        return currentPage != null ? currentPage.getIndex() : 0;
    }

    @Override
    protected void onDestroy() {
        if (currentPage != null) currentPage.close();
        if (renderer != null) renderer.close();
        if (fileDescriptor != null) {
            try { fileDescriptor.close(); } catch (IOException ignored) {}
        }
        super.onDestroy();
    }
}
