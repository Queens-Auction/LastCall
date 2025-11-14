package org.example.lastcall.domain.product.entity;

import org.example.lastcall.common.entity.BaseEntity;
import org.example.lastcall.domain.product.enums.ImageType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "product_image", indexes = {
	@Index(name = "idx_product_image_product_deleted", columnList = "product_id, deleted")
})
public class ProductImage extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(name = "image_type", nullable = false, length = 9)
	@Enumerated(EnumType.STRING)
	private ImageType imageType;

	@Column(name = "image_key", nullable = false, length = 500)
	private String imageKey;

	@Column(name = "file_hash", nullable = false, length = 128)
	private String fileHash;

	private ProductImage(Product product, ImageType imageType, String imageKey, String fileHash) {
		this.product = product;
		this.imageType = imageType;
		this.imageKey = imageKey;
		this.fileHash = fileHash;
	}

	public static ProductImage of(Product product, ImageType imageType, String imageKey, String fileHash) {
		return new ProductImage(product, imageType, imageKey, fileHash);
	}

	public void updateImageType(ImageType imageType) {
		this.imageType = imageType;
	}
}
