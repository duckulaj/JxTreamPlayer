package com.hawkins.xtreamjson.data;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Episode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String episodeId;
    private String seriesId;
    private String seasonId;
    private String name;
    private String episodeNum;
    @Lob
    private String infoJson;
}