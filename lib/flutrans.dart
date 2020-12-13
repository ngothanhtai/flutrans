import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

typedef Future<void> MidtransCallback(TransactionFinished transactionFinished);

class Flutrans {
  MidtransCallback finishCallback;
  static Flutrans _instance = Flutrans._internal();
  static const MethodChannel _channel = const MethodChannel('flutrans');

  Flutrans._internal() {
    _channel.setMethodCallHandler(_channelHandler);
  }

  factory Flutrans() {
    return _instance;
  }

  Future<dynamic> _channelHandler(MethodCall methodCall) async {
    if (methodCall.method == "onTransactionFinished") {
      if (finishCallback != null) {
        await finishCallback(TransactionFinished(
          methodCall.arguments['transactionCanceled'],
          methodCall.arguments['status'],
          methodCall.arguments['source'],
          methodCall.arguments['statusMessage'],
          methodCall.arguments['response'],
        ));
      }
    }
    return Future.value(null);
  }

  void setFinishCallback(MidtransCallback callback) {
    finishCallback = callback;
  }

  Future<void> init(String clientId, String url,
      {String env = 'production'}) async {
    await _channel.invokeMethod("init", {
      "client_key": clientId,
      "base_url": url,
      "env": env,
    });
    return Future.value(null);
  }

  Future<void> makePayment(MidtransTransaction transaction) async {
    await _channel.invokeMethod("payment", jsonEncode(transaction.toJson()));
    return Future.value(null);
  }
}

class MidtransCustomer {
  final String firstName;
  final String lastName;
  final String email;
  final String phone;
  MidtransCustomer(this.firstName, this.lastName, this.email, this.phone);
  MidtransCustomer.fromJson(Map<String, dynamic> json)
      : firstName = json["first_name"],
        lastName = json["last_name"],
        email = json["email"],
        phone = json["phone"];
  Map<String, dynamic> toJson() {
    return {
      "first_name": firstName,
      "last_name": lastName,
      "email": email,
      "phone": phone,
    };
  }
}

class MidtransAddress {
  final String firstName;
  final String lastName;
  final String phone;
  final String address1;
  final String city;
  final String zip;
  final String countryCode;

  MidtransAddress(
    this.firstName,
    this.lastName,
    this.phone,
    this.address1,
    this.city,
    this.zip,
    this.countryCode,
  );

  MidtransAddress.fromJson(Map<String, dynamic> json)
      : firstName = json["first_name"],
        lastName = json["last_name"],
        phone = json["phone"],
        address1 = json["address1"],
        city = json["city"],
        zip = json["zip"],
        countryCode = json["country_code"];
  Map<String, dynamic> toJson() {
    return {
      "first_name": firstName,
      "last_name": lastName,
      "phone": phone,
      "address1": address1,
      "city": city,
      "zip": zip,
      "country_code": countryCode,
    };
  }
}

class MidtransItem {
  final String id;
  final int price;
  final int quantity;
  final String name;
  MidtransItem(this.id, this.price, this.quantity, this.name);
  MidtransItem.fromJson(Map<String, dynamic> json)
      : id = json["id"],
        price = json["price"],
        quantity = json["quantity"],
        name = json["name"];
  Map<String, dynamic> toJson() {
    return {
      "id": id,
      "price": price,
      "quantity": quantity,
      "name": name,
    };
  }
}

class MidtransTransaction {
  final int total;
  final String orderId;
  final MidtransCustomer customer;
  final MidtransAddress address;
  final List<MidtransItem> items;
  final String customField1;
  final String customField2;
  final String customField3;

  MidtransTransaction({
    @required this.total,
    @required this.orderId,
    this.customer,
    this.address,
    this.items = const [],
    this.customField1,
    this.customField2,
    this.customField3,
  });

  Map<String, dynamic> toJson() {
    return {
      "total": total,
      "order_id": orderId,
      "items": items.map((v) => v.toJson()).toList(),
      "customer": customer?.toJson(),
      "address": address?.toJson(),
      "custom_field_1": customField1,
      "custom_field_2": customField2,
      "custom_field_3": customField3,
    };
  }
}

class TransactionFinished {
  final bool transactionCanceled;
  final String status;
  final String source;
  final String statusMessage;
  final String response;
  TransactionFinished(
    this.transactionCanceled,
    this.status,
    this.source,
    this.statusMessage,
    this.response,
  );
}

class PaymentMethod {
  static int CREDIT_CARD = 0;
  static int BANK_TRANSFER = 1;
  static int BANK_TRANSFER_BCA = 2;
  static int BANK_TRANSFER_MANDIRI = 3;
  static int BANK_TRANSFER_PERMATA = 4;
  static int BANK_TRANSFER_BNI = 5;
  static int BANK_TRANSFER_OTHER = 6;
  static int GO_PAY = 7;
  static int BCA_KLIKPAY = 8;
  static int KLIKBCA = 9;
  static int MANDIRI_CLICKPAY = 10;
  static int MANDIRI_ECASH = 11;
  static int EPAY_BRI = 12;
  static int CIMB_CLICKS = 13;
  static int INDOMARET = 14;
  static int KIOSON = 15;
  static int GIFT_CARD_INDONESIA = 16;
  static int INDOSAT_DOMPETKU = 17;
  static int TELKOMSEL_CASH = 18;
  static int XL_TUNAI = 19;
  static int DANAMON_ONLINE = 20;
  static int AKULAKU = 21;
  static int ALFAMART = 22;
}
