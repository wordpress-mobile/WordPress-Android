PRAGMA foreign_keys=OFF;
BEGIN TRANSACTION;
CREATE TABLE android_metadata (locale TEXT);
INSERT INTO "android_metadata" VALUES('en_US');
CREATE TABLE accounts (id integer primary key autoincrement, url text, blogName text, username text, password text, imagePlacement text, centerThumbnail boolean, fullSizeImage boolean, maxImageWidth text, maxImageWidthId integer, lastCommentId integer, runService boolean, blogId integer, location boolean default false, dotcom_username text, dotcom_password text, api_key text, api_blogid text, dotcomFlag boolean default false, wpVersion text, httpuser text, httppassword text, postFormats text default '', isScaledImage boolean default false, scaledImgWidth integer default 1024, homeURL text default '', blog_options text default '', isAdmin boolean default false, isHidden boolean default 0);
INSERT INTO accounts VALUES(8,'',NULL,'','AS3vw/BNTdI=
',NULL,0,0,'',0,NULL,0,0,'false',NULL,NULL,NULL,NULL,0,NULL,'','AS3vw/BNTdI=
','','false',1024,NULL,'',0,0);
INSERT INTO accounts VALUES(9,'',NULL,'','AS3vw/BNTdI=
',NULL,0,0,'',0,NULL,0,0,'false',NULL,NULL,NULL,NULL,0,'3.9-alpha','','AS3vw/BNTdI=
','','false',1024,NULL,'',0,0);
CREATE TABLE posts (id integer primary key autoincrement, blogID text, postid text, title text default '', dateCreated date, date_created_gmt date, categories text default '', custom_fields text default '', description text default '', link text default '', mt_allow_comments boolean, mt_allow_pings boolean, mt_excerpt text default '', mt_keywords text default '', mt_text_more text default '', permaLink text default '', post_status text default '', userid integer default 0, wp_author_display_name text default '', wp_author_id text default '', wp_password text default '', wp_post_format text default '', wp_slug text default '', mediaPaths text default '', latitude real, longitude real, localDraft boolean default 0, uploaded boolean default 0, isPage boolean default 0, wp_page_parent_id text, wp_page_parent_title text, isLocalChange boolean default 0);
CREATE TABLE comments (blogID text, postID text, iCommentID integer, author text, comment text, commentDate text, commentDateFormatted text, status text, url text, email text, postTitle text);
CREATE TABLE cats (id integer primary key autoincrement, blog_id text, wp_id integer, category_name text not null, parent_id integer default 0);
CREATE TABLE quickpress_shortcuts (id integer primary key autoincrement, accountId text, name text);
CREATE TABLE media (id integer primary key autoincrement, postID integer not null, filePath text default '', fileName text default '', title text default '', description text default '', caption text default '', horizontalAlignment integer default 0, width integer default 0, height integer default 0, mimeType text default '', featured boolean default false, isVideo boolean default false, isFeaturedInPost boolean default false, fileURL text default '', thumbnailURL text default '', mediaId text default '', blogId text default '', date_created_gmt date, uploadState default '', videoPressShortcode text default '');
CREATE TABLE themes (_id integer primary key autoincrement, themeId text, name text, description text, screenshotURL text, trendingRank integer default 0, popularityRank integer default 0, launchDate date, previewURL text, blogId text, isCurrent boolean default false, isPremium boolean default false, features text);
CREATE TABLE notes (id integer primary key, note_id text, message text, type text, raw_note_data text, timestamp integer, placeholder boolean);
DELETE FROM sqlite_sequence;
INSERT INTO "sqlite_sequence" VALUES('accounts',9);
COMMIT;
