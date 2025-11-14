package org.example.lastcall.domain.product.entity;

import org.example.lastcall.common.entity.BaseEntity;
import org.example.lastcall.domain.product.enums.Category;
import org.example.lastcall.domain.user.entity.User;

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

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "products", indexes = {
	@Index(name = "idx_product_user_deleted_created", columnList = "user_id, deleted, created_at DESC"),
	@Index(name = "idx_product_category_created", columnList = "category, created_at DESC"),
})
public class Product extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "name", nullable = false, length = 80)
	private String name;

	@Column(name = "category", nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private Category category;

	@Column(name = "description", nullable = false, length = 500)
	private String description;

	private Product(User user, String name, Category category, String description) {
		this.user = user;
		this.name = name;
		this.category = category;
		this.description = description;
	}

	public static Product of(User user, String name, Category category, String description) {
		return new Product(user, name, category, description);
	}

	public void updateProducts(String name, Category category, String description) {
		if (name != null && !name.isBlank()) {
			this.name = name;
		}

		if (category != null) {
			this.category = category;
		}

		if (description != null && !description.isBlank()) {
			this.description = description;
		}
	}
}
