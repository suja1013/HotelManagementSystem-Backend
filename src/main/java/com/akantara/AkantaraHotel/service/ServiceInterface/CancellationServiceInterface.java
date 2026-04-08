package com.akantara.AkantaraHotel.service.ServiceInterface;

import com.akantara.AkantaraHotel.dto.Response;


public interface CancellationServiceInterface {

    Response cancelBooking(Long bookingId);

    Response previewCancellation(Long bookingId);

}
