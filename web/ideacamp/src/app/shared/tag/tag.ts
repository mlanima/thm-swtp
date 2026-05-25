import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-tag',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './tag.html'
})
export class TagComponent {

  tags = [
    { label: 'Frontend', color: 'bg-[#84bd22] text-white' },
    { label: 'Backend', color: 'bg-[#5b9b20] text-white' },
    { label: 'UI/UX', color: 'bg-[#f1cd75] text-black' },
    { label: 'DevOps', color: 'bg-[#f58a42] text-black' },
    { label: 'Mobile', color: 'bg-[#2c3e50] text-white' },
  ];
}
