package com.blueshift.reads.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.blueshift.Blueshift;
import com.blueshift.model.Product;
import com.blueshift.model.UserInfo;
import com.blueshift.reads.R;
import com.blueshift.reads.ShoppingCart;
import com.blueshift.reads.model.Book;
import com.github.rahulrvp.android_utils.EditTextUtils;
import com.github.rahulrvp.android_utils.TextViewUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PlaceOrderActivity extends AppCompatActivity {

    private ShoppingCart mCart;
    private TextView mTotalNoTax;
    private TextView mTotalWithTax;
    private TextInputLayout mNameTIL;
    private TextInputLayout mEmailTIL;
    private TextInputLayout mCompanyTIL;
    private TextInputLayout mContactTIL;
    private String mName;
    private String mEmail;
    private String mContact;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_order);

        setTitle(R.string.review_products);

        mTotalNoTax = (TextView) findViewById(R.id.total_excl_tax);
        mTotalWithTax = (TextView) findViewById(R.id.total_with_tax);

        mNameTIL = (TextInputLayout) findViewById(R.id.order_name);
        mEmailTIL = (TextInputLayout) findViewById(R.id.order_email);
        mCompanyTIL = (TextInputLayout) findViewById(R.id.order_company);
        mContactTIL = (TextInputLayout) findViewById(R.id.order_contact);

        RecyclerView productRView = (RecyclerView) findViewById(R.id.cart_product_list);
        productRView.setLayoutManager(new LinearLayoutManager(this));

        CartProductsAdapter rVAdapter = new CartProductsAdapter();
        productRView.setAdapter(rVAdapter);

        mCart = ShoppingCart.getInstance(this);
        rVAdapter.setBooks(mCart.getBooks());

        UserInfo userInfo = UserInfo.getInstance(this);
        EditTextUtils.setText(mEmailTIL.getEditText(), userInfo.getEmail());

        updateSummaryView();
    }

    @Override
    protected void onPause() {
        super.onPause();

        ShoppingCart.getInstance(this).save(this);
    }

    private void updateSummaryView() {
        Float totalAmt = mCart.getTotalAmount();
        String totalAmtStr = String.format(Locale.getDefault(), "%.2f", totalAmt);

        TextViewUtils.setText(mTotalNoTax, R.string.dollar_x, totalAmtStr);
        TextViewUtils.setText(mTotalWithTax, R.string.dollar_x, totalAmtStr);
    }

    public void onPlaceOrderClick(View view) {
        if (hasValidParams()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Checkout Cart");
            builder.setMessage("Place the order?");
            builder.setNegativeButton("No", null);
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    placeOrder();
                }
            });
            builder.create().show();
        }
    }

    private void placeOrder() {
        List<Product> productList = new ArrayList<>();
        List<Book> bookList = mCart.getBooks();
        for (Book book : bookList) {
            Product product = new Product();
            product.setSku(book.getSku());
            product.setPrice(Float.valueOf(book.getPrice()));
            product.setQuantity(book.getQuantity());

            productList.add(product);
        }

        Product[] products = new Product[0];
        productList.toArray(products);

        Float totalAmt = mCart.getTotalAmount();

        Blueshift
                .getInstance(this)
                .trackCheckoutCart(products, totalAmt, 0f, null, false);

        // Place order success.
        long orderId = System.currentTimeMillis();

        Blueshift
                .getInstance(this)
                .trackProductsPurchase(String.valueOf(orderId), products, totalAmt, 0f, 0f, null, false);

        mCart.clear();

        dialog = new ProgressDialog(this);

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                dialog.setCancelable(false);
                dialog.setMessage("Placing order...");
                dialog.show();
            }

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                dialog.dismiss();

                showSuccessDialog();
            }
        }.execute();
    }

    private void showSuccessDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.app_name);
        builder.setMessage("Order placed successfully.");
        builder.setPositiveButton("Ok", null);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                finish();
            }
        });
        builder.create().show();
    }

    private boolean hasValidParams() {
        mName = EditTextUtils.getText(mNameTIL.getEditText());
        if (TextUtils.isEmpty(mName)) {
            mNameTIL.setError("Please enter a name");
            return false;
        }

        mEmail = EditTextUtils.getText(mEmailTIL.getEditText());
        if (TextUtils.isEmpty(mEmail)) {
            mEmailTIL.setError("Please enter a valid email");
            return false;
        }

        mContact = EditTextUtils.getText(mContactTIL.getEditText());
        if (TextUtils.isEmpty(mContact)) {
            mContactTIL.setError("Please enter a contact");
            return false;
        }

        return true;
    }

    private class CartProductsAdapter extends RecyclerView.Adapter<CartProductsAdapter.ViewHolder> {

        private final List<Book> mBooks = new ArrayList<>();

        void setBooks(List<Book> books) {
            mBooks.addAll(books);

            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_book_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.fillValues(mBooks.get(position), position);
        }

        @Override
        public int getItemCount() {
            return mBooks.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            private final TextView mNameText;
            private final TextView mPriceText;
            private final TextView mQuantityText;
            private final Button mPlusBtn;
            private final Button mMinusBtn;
            private final ImageButton mDeleteBtn;

            ViewHolder(View itemView) {
                super(itemView);

                mNameText = (TextView) itemView.findViewById(R.id.cart_item_book_name);
                mPriceText = (TextView) itemView.findViewById(R.id.cart_item_book_price);
                mQuantityText = (TextView) itemView.findViewById(R.id.cart_item_book_quantity);

                mPlusBtn = (Button) itemView.findViewById(R.id.qty_increase_btn);
                mMinusBtn = (Button) itemView.findViewById(R.id.qty_decrease_btn);
                mDeleteBtn = (ImageButton) itemView.findViewById(R.id.cart_item_delete_book_btn);
            }

            void fillValues(Book book, final int position) {
                if (book != null) {
                    TextViewUtils.setText(mNameText, book.getName());
                    TextViewUtils.setText(mPriceText, R.string.dollar_x, book.getPrice());
                    TextViewUtils.setText(mQuantityText, String.valueOf(book.getQuantity()));

                    if (mPlusBtn != null) {
                        mPlusBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                increaseQty(position);
                            }
                        });
                    }

                    if (mMinusBtn != null) {
                        mMinusBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                decreaseQty(position);
                            }
                        });
                    }

                    if (mDeleteBtn != null) {
                        mDeleteBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                deleteItem(position);
                            }
                        });
                    }
                }
            }

            private void decreaseQty(int position) {
                Book book = mBooks.get(position);
                if (book.getQuantity() > 1) {
                    int qty = mCart.decreaseQuantity(book);

                    TextViewUtils.setText(mQuantityText, String.valueOf(qty));

                    updateSummaryView();
                }
            }

            private void increaseQty(int position) {
                Book book = mBooks.get(position);

                int qty = mCart.increaseQuantity(book);

                TextViewUtils.setText(mQuantityText, String.valueOf(qty));

                updateSummaryView();
            }

            private void deleteItem(int position) {
                Book book = mBooks.get(position);
                mCart.remove(book);

                mBooks.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, mBooks.size());

                updateSummaryView();
            }
        }
    }
}
