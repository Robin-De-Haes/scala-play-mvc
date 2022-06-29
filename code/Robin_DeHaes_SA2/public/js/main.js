/* Function to setup headers for Ajax calls (i.e. set the CSRF token in the header for POST/DELETE).
 * It gets the CSRF token from an input field and add its to the header.
 */
function setupAjax() {
    let token =  $('input[name="csrfToken"]').attr('value')
    $.ajaxSetup({
        beforeSend: function(xhr) {
            xhr.setRequestHeader('Csrf-Token', token);
        }
    });
}

/* Function to make the likeBtns clickable. Without calling this function the likeBtns will just be informative
*  and show the current like count, without the option to like/unlike a post. */
function activateLikeBtns(likeBtns = $('.like-btn') ) {
    likeBtns.addClass("activated") /* Used for css differences between clickable and non-clickable like-btns */

    likeBtns.click(function() {
        let clickedLike = $(this), postID = $(this).attr('data-post-id')

        setupAjax()

        $.ajax(jsRoutes.controllers.PostController.toggleLike(postID))
            .done(function(data){
                /* replace the like btn with the updated version */
                let newLikeBtn = $(data)
                clickedLike.replaceWith(newLikeBtn)
                activateLikeBtns(likeBtns = newLikeBtn)
            })
            .fail( function(jqXHR, textStatus, errorThrown){
                alert("Something went wrong. Please try again!")
            });
    });

    /* activate like btn tooltips */
    likeBtns.find('[data-toggle="tooltip"]').tooltip();
}