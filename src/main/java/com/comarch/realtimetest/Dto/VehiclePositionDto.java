package com.comarch.realtimetest.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehiclePositionDto {
    private String id;
    private float latitude;
    private float longitude;
}
