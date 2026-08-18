package com.example.artwork_3d_frontend.dto;

import lombok.Data;

@Data
public class ArtworkPublishDTO {
    private String title;
    private String author;
    private String year;
    private String description;
    private String url;
}