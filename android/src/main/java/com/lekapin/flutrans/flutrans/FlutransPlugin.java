package com.lekapin.flutrans.flutrans;
import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;
import com.midtrans.sdk.corekit.callback.TransactionFinishedCallback;
import com.midtrans.sdk.corekit.core.MidtransSDK;
import com.midtrans.sdk.corekit.core.PaymentMethod;
import com.midtrans.sdk.corekit.core.TransactionRequest;
import com.midtrans.sdk.corekit.core.UIKitCustomSetting;
import com.midtrans.sdk.corekit.core.themes.CustomColorTheme;
import com.midtrans.sdk.corekit.models.BillingAddress;
import com.midtrans.sdk.corekit.models.CustomerDetails;
import com.midtrans.sdk.corekit.models.ItemDetails;
import com.midtrans.sdk.corekit.models.ShippingAddress;
import com.midtrans.sdk.corekit.models.snap.CreditCard;
import com.midtrans.sdk.corekit.models.snap.TransactionResult;
import com.midtrans.sdk.uikit.SdkUIFlowBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** FlutransPlugin */
public class FlutransPlugin implements MethodCallHandler, TransactionFinishedCallback {
  static final String TAG = "FlutransPlugin";
  private final Registrar registrar;
  private final MethodChannel channel;
  private Context context;
  private Result flutterResult;

  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutrans");
    channel.setMethodCallHandler(new FlutransPlugin(registrar, channel));
  }

  private FlutransPlugin(Registrar registrar, MethodChannel channel) {
    this.registrar = registrar;
    this.channel = channel;
    this.context = registrar.activeContext();
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    flutterResult = result;
    if(call.method.equals("init")) {
      initMidtransSdk((String)call.argument("client_key").toString(), call.argument("base_url").toString());
      result.success(null);
    } else if(call.method.equals("payment")) {
      String str = call.arguments();
      payment(str);
    } else {
      result.notImplemented();
    }
  }

  private void initMidtransSdk(String client_key, String base_url) {
    SdkUIFlowBuilder.init()
            .setClientKey(client_key) // client_key is mandatory
            .setContext(context) // context is mandatory
            .setTransactionFinishedCallback(this) // set transaction finish callback (sdk callback)
            .setMerchantBaseUrl(base_url) //set merchant url
            .enableLog(true) // enable sdk log
            .buildSDK();
  }

  void payment(String str) {
    try {
        Log.d(TAG, str);
      JSONObject json = new JSONObject(str);

      TransactionRequest transactionRequest = new
              TransactionRequest(json.getString("order_id"), json.getInt("total"));
      ArrayList<ItemDetails> itemList = new ArrayList<>();
      JSONArray arr = json.getJSONArray("items");
      for(int i = 0; i < arr.length(); i++) {
        JSONObject obj = arr.getJSONObject(i);
        ItemDetails item = new ItemDetails(obj.getString("id"), obj.getInt("price"), obj.getInt("quantity"), obj.getString("name"));
        itemList.add(item);
      }

      if (!json.isNull("customer")) {
        JSONObject cJson = json.getJSONObject("customer");
        CustomerDetails cus = new CustomerDetails();
        cus.setFirstName(cJson.getString("first_name"));
        cus.setLastName(cJson.getString("last_name"));
        cus.setEmail(cJson.getString("email"));
        cus.setPhone(cJson.getString("phone"));


        if (!json.isNull("address")) {
          JSONObject address = json.getJSONObject("address");
          String firstName = address.getString("first_name");
          String lastName = address.getString("last_name");
          String phone = address.getString("phone");
          String address1 = address.getString("address1");
          String city = address.getString("city");
          String zip = address.getString("zip");
          String countryCode = address.getString("country_code");
          BillingAddress billingAddress = new BillingAddress();
          billingAddress.setFirstName(firstName);
          billingAddress.setLastName(lastName);
          billingAddress.setPhone(phone);
          billingAddress.setAddress(address1);
          billingAddress.setCity(city);
          billingAddress.setPostalCode(zip);
          billingAddress.setCountryCode(countryCode);

          ShippingAddress shippingAddress = new ShippingAddress();
          shippingAddress.setFirstName(firstName);
          shippingAddress.setLastName(lastName);
          shippingAddress.setPhone(phone);
          shippingAddress.setAddress(address1);
          shippingAddress.setCity(city);
          shippingAddress.setPostalCode(zip);
          shippingAddress.setCountryCode(countryCode);

          cus.setBillingAddress(billingAddress);
          cus.setShippingAddress(shippingAddress);
        }

        transactionRequest.setCustomerDetails(cus);
      }

      if(json.has("custom_field_1"))
        transactionRequest.setCustomField1(json.getString("custom_field_1"));
      if(json.has("custom_field_2"))
        transactionRequest.setCustomField2(json.getString("custom_field_2"));
      if(json.has("custom_field_3"))
        transactionRequest.setCustomField3(json.getString("custom_field_3"));
      transactionRequest.setItemDetails(itemList);

      CreditCard creditCardOptions = new CreditCard();
      creditCardOptions.setSaveCard(true);

      transactionRequest.setCreditCard(creditCardOptions);

      UIKitCustomSetting setting = MidtransSDK.getInstance().getUIKitCustomSetting();
      setting.setSkipCustomerDetailsPages(true);

      MidtransSDK.getInstance().setUIKitCustomSetting(setting);
      MidtransSDK.getInstance().setTransactionRequest(transactionRequest);
      MidtransSDK.getInstance().startPaymentUiFlow(context);
    } catch(Exception e) {
      Log.d(TAG, "ERROR " + e.getMessage());
    }
  }

  @Override
  public void onTransactionFinished(TransactionResult transactionResult) {
      Map<String, Object> content = new HashMap<>();
      content.put("transactionCanceled", transactionResult.isTransactionCanceled());
      content.put("status", transactionResult.getStatus());
      content.put("source", transactionResult.getSource());
      content.put("statusMessage", transactionResult.getStatusMessage());
      if(transactionResult.getResponse() != null)
        content.put("response", transactionResult.getResponse().toString());
      else
        content.put("response", null);
      channel.invokeMethod("onTransactionFinished", content);

      flutterResult.success(null);
  }
}
