package com.mulmi.backend.domain.rental.repository;

import com.mulmi.backend.domain.rental.entity.RentalImage;
import com.mulmi.backend.domain.rental.enums.RentalImageSide;
import com.mulmi.backend.domain.rental.enums.RentalImageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RentalImageRepository extends JpaRepository<RentalImage, Long> {

    // 같은 대여의 같은 종류·방향 사진 (있으면 교체, 없으면 새로 저장)
    Optional<RentalImage> findByRentalIdAndImageTypeAndImageSide(
            Long rentalId, RentalImageType imageType, RentalImageSide imageSide);

    // 수령(54)·반납(57) 처리 전 사진이 있는지
    boolean existsByRentalIdAndImageTypeAndImageSide(
            Long rentalId, RentalImageType imageType, RentalImageSide imageSide);
}
