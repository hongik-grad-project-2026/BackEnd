package com.mulmi.backend.domain.rental.entity;

import com.mulmi.backend.domain.rental.enums.RentalImageSide;
import com.mulmi.backend.domain.rental.enums.RentalImageType;
import com.mulmi.backend.domain.user.entity.User;
import com.mulmi.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// 대여 수령·반납 사진 (명세 55·56, ERD RENTAL_IMAGE)
// 같은 대여의 같은 종류·방향 사진은 1장만 : 다시 올리면 새 행이 아니라 교체한다
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "rental_image",
        uniqueConstraints = @UniqueConstraint(columnNames = {"rental_id", "image_type", "image_side"}))
public class RentalImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rental_image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rental_id", nullable = false)
    private Rental rental;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 30)
    private RentalImageType imageType;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Enumerated(EnumType.STRING)
    @Column(name = "image_side", nullable = false, length = 30)
    private RentalImageSide imageSide;

    // S3 객체 키 (모델 이미지와 같은 방식 : 응답 때 임시 조회 URL로 바꾼다)
    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    // 사진을 올린 근로생·조교
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    // 교체 : 수령·반납 처리 전에 잘못 찍은 사진을 바꾼다 (ERD 사진 삭제 정책)
    public void replace(String objectKey, User uploader) {
        this.imageUrl = objectKey;
        this.uploadedBy = uploader;
    }
}
