package lk.menara.studysphere.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review implements Serializable {

    private String userName;
    private String comment;
    private float rating;
    private String date;
    private String userImage;
}