<!DOCTYPE html>
<html lang="en">

<body>

<div class="container">
    <div class="download">
        <ul>
        <#list content.downloads.items as download>
            <li>
                <p><a href="${download.href}"><span class="icon"><i class="fa fa-download"></i></span> ${download.displayName}</a></p>
            </li>
        </#list>
        </ul>
    </div>
    <div class="row">
        <div class="col-lg-8 col-lg-offset-2 col-md-10 col-md-offset-1">
            <article role="main" class="blog-post">
                ${content.body}
            </article>
        </div>
    </div>
</div>

</body>
</html>