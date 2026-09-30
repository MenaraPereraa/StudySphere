package lk.menara.studysphere.model;

import com.google.firebase.firestore.PropertyName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Category {
    private String categoryId;
    private String name;
    private String description;

    @PropertyName("imageUrl")
    private String imageUrl;
}