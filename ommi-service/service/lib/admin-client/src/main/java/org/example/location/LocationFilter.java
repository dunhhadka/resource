package org.example.location;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class LocationFilter {

    private boolean defaultLocation;
    private List<Integer> ids;
}
