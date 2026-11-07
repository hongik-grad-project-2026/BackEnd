package com.mulmi.backend.domain.rental.repository;

import com.mulmi.backend.domain.rental.entity.RentalImage;
import com.mulmi.backend.domain.rental.enums.RentalImageSide;
import com.mulmi.backend.domain.rental.enums.RentalImageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RentalImageRepository extends JpaRepository<RentalImage, Long> {

    // 같은 대여의 같은 종류·방향 사진 (있으면 교체, 없으면 새로 저장)
    Optional<RentalImage> findByRentalIdAndImageTypeAndImageSide(
            Long rentalId, RentalImageType imageType, RentalImageSide imageSide);

    // 대여 한 건의 사진 전부 (48 상세 : 수령·반납 사진, 최대 2장)
    List<RentalImage> findByRentalId(Long rentalId);

    // 수령(54)·반납(57) 처리 전 사진이 있는지
    boolean existsByRentalIdAndImageTypeAndImageSide(
            Long rentalId, RentalImageType imageType, RentalImageSide imageSide);
}
