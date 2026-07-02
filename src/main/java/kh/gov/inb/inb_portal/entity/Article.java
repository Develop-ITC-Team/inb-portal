package kh.gov.inb.inb_portal.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Table(name = "article")
@Entity
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private  String title;

    @Column(nullable = false, length = 255)
    private  String content;

    @Column(length = 100)
    private String author;

    @Column(length = 50)
    private String status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createAt;

    @UpdateTimestamp
    private LocalDateTime updateAt;

}
