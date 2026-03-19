import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-news-page',
  imports: [],
  templateUrl: './news-page.html',
  styleUrl: './news-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NewsPage {}
