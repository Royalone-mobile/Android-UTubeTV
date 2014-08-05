package com.distantfuture.videos.services;

import android.content.Context;
import android.os.Bundle;

public class SubscriptionsServiceRequest {
  private static final int CLASS_TYPE_KEY = 5544;
  ServiceRequest serviceRequest;

  private SubscriptionsServiceRequest(ServiceRequest serviceRequest) {
    super();

    this.serviceRequest = serviceRequest;
  }

  public SubscriptionsServiceRequest() {
    super();

    serviceRequest = new ServiceRequest();
    serviceRequest.putInt(ServiceRequest.REQUEST_CLASS_TYPE_KEY, CLASS_TYPE_KEY);
  }

  public static SubscriptionsServiceRequest fromBundle(Bundle bundle) {
    SubscriptionsServiceRequest result = null;
    ServiceRequest request = ServiceRequest.fromBundle(bundle);

    Integer intValue = (Integer) request.getData(ServiceRequest.REQUEST_CLASS_TYPE_KEY);

    if (intValue != null) {
      if (intValue == CLASS_TYPE_KEY)
        result = new SubscriptionsServiceRequest(request);
    }

    return result;
  }

  public void runTask(Context context) {
    new SubscriptionsServiceTask(context, this);
  }

  public Bundle toBundle() {
    return ServiceRequest.toBundle(serviceRequest);
  }
}
